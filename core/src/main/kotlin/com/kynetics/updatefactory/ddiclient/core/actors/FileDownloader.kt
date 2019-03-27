package com.kynetics.updatefactory.ddiclient.core.actors

import com.kynetics.updatefactory.ddiapiclient.api.DdiClient
import com.kynetics.updatefactory.ddiclient.core.actors.FileDownloader.Companion.Message.*
import com.kynetics.updatefactory.ddiclient.core.api.EventListener
import com.kynetics.updatefactory.ddiclient.core.md5
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.launch
import java.io.File

@UseExperimental(ObsoleteCoroutinesApi::class)
class FileDownloader
private constructor(scope: ActorScope,
                    private val fileToDownload: FileToDownload,
                    attempts: Int): AbstractActor(scope) {

    private val client: DdiClient = coroutineContext[UFClientContext]!!.ddiClient
    private val notificationManager = coroutineContext[NMActor]!!.ref

    private fun beforeStart(state: State): Receive = { msg ->
        when(msg) {

            is Start ->{
                if( fileToDownload.destination.exists()){
                    parent!!.send(AlreadyDownloaded(channel, fileToDownload.md5))
                    channel.close()
                } else {
                    become(downloading(state))
                    notificationManager.send(EventListener.Event.StartDownloadFile(fileToDownload.md5))
                    tryDownload(state)
                }
            }

            is Stop  -> this.cancel()

            else             -> unhandled(msg)

        }

    }

    private fun downloading(state: State): Receive = { msg ->

        when(msg) {

            is FileDownloaded -> checkMd5OfDownloadedFile()

            is FileChecked -> {
                parent!!.send(Success(channel, fileToDownload.md5))
                notificationManager.send(EventListener.Event.FileDownloaded(fileToDownload.md5))
            }

            is TrialExhausted -> {
                parent!!.send(Error(channel, fileToDownload.md5, "trials exhausted due to errors: ${state.errors.joinToString("\n", "\n")}"))
                notificationManager.send(EventListener.Event.Error(listOf("Fail to download file at ${fileToDownload.url}")))
            }

            is RetryDownload     -> {
                val errorMessage = "retry download due to: ${msg.cause}"
                parent!!.send(Info(channel, fileToDownload.md5, errorMessage))
                notificationManager.send(EventListener.Event.Error(listOf(errorMessage, "Remaining attempts: ${state.remainingAttempts}")))
                val newState = state.copy(remainingAttempts = state.remainingAttempts-1, errors = state.errors+msg.cause)
                become(downloading(newState))
                tryDownload(newState)
            }

            is Message.Stop      -> this.cancel()

            else                 -> unhandled(msg)

        }

    }

    private suspend fun tryDownload(state: State){
        if(state.remainingAttempts <= 0) {
            channel.send(TrialExhausted)
        } else {
            launch {
                try {
                    download()
                    channel.send(FileDownloaded)
                } catch (t:Throwable){
                    channel.send(RetryDownload("exception: ${t.javaClass.simpleName}. message: ${t.message}"))
                }
            }
        }
    }

    private suspend fun download() {
        val file = fileToDownload.tempFile
        if(file.exists()){
            file.delete()
        }
        file.outputStream().use {
            client.downloadArtifact(fileToDownload.url).copyTo(it)
        }
    }

    private suspend fun checkMd5OfDownloadedFile() {
        launch {
            val file = fileToDownload.tempFile
            when {

                file.md5() == fileToDownload.md5 -> {
                    fileToDownload.onFileSaved()
                    channel.send(FileChecked)
                }

                file.delete() -> channel.send(Companion.Message.RetryDownload("unable verify md5 sum of downloaded file"))

                else -> throw IllegalStateException("UNABLE DELETE FILE $file")
            }
        }
    }

    init {
        become (beforeStart(State(attempts)))
    }

    companion object {
        const val DOWNLOADING_EXTENSION = "downloading"
        fun of(scope: ActorScope,
               attempts: Int,
               fileToDownload: FileToDownload) = FileDownloader(scope, fileToDownload, attempts)

        data class FileToDownload(val md5: String,
                                  val url: String,
                                  val folder: File){
            val tempFile = File(folder, "$md5.$DOWNLOADING_EXTENSION")
            val destination = File(folder, md5)
            fun onFileSaved() = tempFile.renameTo(destination)
        }

        private data class State(
                val remainingAttempts:Int,
                val errors: List<String> = emptyList())

        sealed class Message {

            object Start: Message()
            object Stop : Message()

            object FileDownloaded: Message()
            object FileChecked: Message()
            data class RetryDownload(val cause: String): Message()
            object TrialExhausted: Message()

            data class Success(val sender:ActorRef, val md5: String): Message()
            data class AlreadyDownloaded(val sender:ActorRef, val md5: String): Message()
            data class Info(val sender:ActorRef, val md5: String, val message:String): Message()
            data class Error(val sender:ActorRef, val md5: String, val message:String): Message()
        }
    }
}