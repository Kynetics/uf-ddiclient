package com.kynetics.updatefactory.ddiclient.core.actors

import com.kynetics.updatefactory.ddiapiclient.api.DdiClient
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplFdbkReq
import com.kynetics.updatefactory.ddiclient.core.api.MessageListener
import com.kynetics.updatefactory.ddiclient.core.inputstream.FilterInputStreamWithProgress
import com.kynetics.updatefactory.ddiclient.core.md5
import java.io.File
import java.text.NumberFormat
import java.util.Timer
import java.util.concurrent.ArrayBlockingQueue
import kotlin.concurrent.fixedRateTimer
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.launch

@UseExperimental(ObsoleteCoroutinesApi::class)
class FileDownloader
private constructor(
    scope: ActorScope,
    private val fileToDownload: FileToDownload,
    attempts: Int,
    actionId: String
) : AbstractActor(scope) {

    private val client: DdiClient = coroutineContext[UFClientContext]!!.ddiClient
    private val notificationManager = coroutineContext[NMActor]!!.ref
    private val connectionManager = coroutineContext[CMActor]!!.ref

    private fun beforeStart(state: State): Receive = { msg ->
        when (msg) {

            is Message.Start -> {
                if (fileToDownload.destination.exists()) {
                    parent!!.send(Message.AlreadyDownloaded(channel, fileToDownload.md5))
                } else {
                    become(downloading(state))
                    notificationManager.send(MessageListener.Message.Event.StartDownloadFile(fileToDownload.fileName))
                    tryDownload(state)
                }
            }

            is Message.Stop -> this.cancel()

            else -> unhandled(msg)
        }
    }

    private fun downloading(state: State): Receive = { msg ->

        when (msg) {

            is Message.FileDownloaded -> checkMd5OfDownloadedFile()

            is Message.FileChecked -> {
                parent!!.send(Message.Success(channel, fileToDownload.md5))
                notificationManager.send(MessageListener.Message.Event.FileDownloaded(fileToDownload.fileName))
            }

            is Message.TrialExhausted -> {
                val errors = state.errors.toMutableList()
                errors.add(0, "trials exhausted due to errors (${fileToDownload.fileName})")
                parent!!.send(Message.Error(channel, fileToDownload.md5, errors))
                notificationManager.send(MessageListener.Message.Event.Error(errors))
            }

            is Message.RetryDownload -> {
                val errorMessage = "retry download due to: ${msg.cause}"
                parent!!.send(Message.Info(channel, fileToDownload.md5, errorMessage))
                notificationManager.send(MessageListener.Message.Event.Error(listOf(errorMessage, "Remaining attempts: ${state.remainingAttempts}")))
                val newState = state.copy(remainingAttempts = state.remainingAttempts - 1, errors = state.errors + msg.cause)
                become(downloading(newState))
                tryDownload(newState)
            }

            is Message.Stop -> this.cancel()

            else -> unhandled(msg)
        }
    }

    private suspend fun tryDownload(state: State) {
        if (state.remainingAttempts <= 0) {
            channel.send(Message.TrialExhausted)
        } else {
            launch {
                try {
                    download(state.actionId)
                    channel.send(Message.FileDownloaded)
                } catch (t: Throwable) {
                    channel.send(Message.RetryDownload("exception: ${t.javaClass.simpleName}. message: ${t.message}"))
                    LOG.warn("Failed to download file ${fileToDownload.fileName}", t)
                }
            }
        }
    }

    private fun Double.toPercentage(minFractionDigits: Int = 0): String {
        val format = NumberFormat.getPercentInstance()
        format.minimumFractionDigits = minFractionDigits
        return format.format(this)
    }

    private suspend fun download(actionId: String) {
        val file = fileToDownload.tempFile
        if (file.exists()) {
            file.delete()
        }

        val inputStream = FilterInputStreamWithProgress(client.downloadArtifact(fileToDownload.url), fileToDownload.size)

        val queue = ArrayBlockingQueue<Double>(10, true, (1..9).map { it.toDouble() / 10 })

        val timer = checkDownloadProgress(inputStream, queue, actionId)

        file.outputStream().use {
            inputStream.copyTo(it)
        }

        timer.purge()
        timer.cancel()
    }

    private fun checkDownloadProgress(
        inputStream: FilterInputStreamWithProgress,
        queue: ArrayBlockingQueue<Double>,
        actionId: String
    ): Timer {
        return fixedRateTimer("Download Checker ${fileToDownload.fileName}", false, 60_000, 60_000) {
            launch {
                val progress = inputStream.getProgress()
                val limit = queue.peek() ?: 1.0
                if (progress > limit) {
                    feedback(actionId,
                            DeplFdbkReq.Sts.Exc.proceeding,
                            DeplFdbkReq.Sts.Rslt.Prgrs(0, 0),
                            DeplFdbkReq.Sts.Rslt.Fnsh.none,
                            "Downloading file named ${fileToDownload.fileName} " +
                                    "- ${progress.toPercentage(2)}")
                    while (progress > queue.peek() ?: 1.0 && queue.isNotEmpty()) {
                        queue.poll()
                    }
                }
                notificationManager.send(MessageListener.Message.Event.DownloadProgress(fileToDownload.fileName, progress))
            }
        }
    }

    private suspend fun feedback(id: String, execution: DeplFdbkReq.Sts.Exc, progress: DeplFdbkReq.Sts.Rslt.Prgrs, finished: DeplFdbkReq.Sts.Rslt.Fnsh, vararg messages: String) {
        val deplFdbkReq = DeplFdbkReq.newInstance(id, execution, progress, finished, *messages)
        connectionManager.send(ConnectionManager.Companion.Message.In.DeploymentFeedback(deplFdbkReq))
    }

    private suspend fun checkMd5OfDownloadedFile() {
        launch {
            val file = fileToDownload.tempFile
            val md5 = file.md5()
            when {

                md5 == fileToDownload.md5 -> {
                    fileToDownload.onFileSaved()
                    channel.send(Message.FileChecked)
                }

                file.delete() -> channel.send(Companion.Message.RetryDownload("Downloaded file (${fileToDownload.fileName}) has wrong md5 sum ($md5)"))

                else -> throw IllegalStateException("UNABLE DELETE FILE $file")
            }
        }
    }

    init {
        become(beforeStart(State(attempts, actionId)))
    }

    companion object {
        const val DOWNLOADING_EXTENSION = "downloading"
        fun of(
            scope: ActorScope,
            attempts: Int,
            fileToDownload: FileToDownload,
            actionId: String
        ) = FileDownloader(scope, fileToDownload, attempts, actionId)

        data class FileToDownload(
            val fileName: String,
            val md5: String,
            val url: String,
            val folder: File,
            val size: Long
        ) {
            val tempFile = File(folder, "$md5.$DOWNLOADING_EXTENSION")
            val destination = File(folder, md5)
            fun onFileSaved() = tempFile.renameTo(destination)
        }

        private data class State(
            val remainingAttempts: Int,
            val actionId: String,
            val errors: List<String> = emptyList()
        )

        sealed class Message {

            object Start : Message()
            object Stop : Message()

            object FileDownloaded : Message()
            object FileChecked : Message()
            data class RetryDownload(val cause: String) : Message()
            object TrialExhausted : Message()

            data class Success(val sender: ActorRef, val md5: String) : Message()
            data class AlreadyDownloaded(val sender: ActorRef, val md5: String) : Message()
            data class Info(val sender: ActorRef, val md5: String, val message: String) : Message()
            data class Error(val sender: ActorRef, val md5: String, val message: List<String> = emptyList()) : Message()
        }
    }
}
