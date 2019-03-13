package com.kynetics.updatefactory.ddiclient.core

import com.kynetics.updatefactory.ddiapiclient.api.IDdiClient
import com.kynetics.updatefactory.ddiclient.core.DownloadManager.Companion.Message.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ActorScope
import kotlinx.coroutines.launch
import java.io.File
import kotlin.coroutines.CoroutineContext

class DownloadManager
@UseExperimental(ObsoleteCoroutinesApi::class)
private constructor(scope: ActorScope<Any>,
                    private val fileToDownload: FileToDownload,
                    private val attempts: Int,
                    private val client: IDdiClient): Actor(scope) {

    private fun beforeStart(state:State): Receive = { msg ->
        when(msg) {

            is Message.Start ->{
                become(downloading(tryDownload(state).copy(listener = msg.listener)))
            }

            is Message.Stop  -> become(finished(state.copy(result = Result.ABORT)))

            else             -> unhandled(msg)

        }

    }

    private fun downloading(state:State): Receive = { msg ->

        when(msg) {

            is FileDownloaded -> become(downloading(checkMd5OfDownloadedFile(state)))

            is FileChecked -> {
                state.listener!!.send(Success(channel, fileToDownload.md5))
                become(finished(state.copy(result = Result.SUCCESS)))
            }

            is TrialExhausted -> {
                state.listener!!.send(Error(channel, fileToDownload.md5, "trials exhausted due to errors: ${state.errors.joinToString("\n", "\n")}"))
                become(finished(state.copy(result = Result.ERROR)))
            }

            is RetryDownload     -> {
                state.listener!!.send(Info(channel, fileToDownload.md5, "retry download due to: ${msg.cause}"))
                val newState = state.copy(remainingAttempts = state.remainingAttempts-1, errors = state.errors+msg.cause)
                become(downloading(newState))
                tryDownload(newState)
            }

            is Message.Stop      -> {
                state.pendingJob?.cancel()
                become(finished(state.copy(result = Result.ABORT)))
            }

            else                 -> unhandled(msg)

        }

    }

    private fun finished(state:State): Receive = { msg -> unhandled(msg) }

    private suspend fun tryDownload(state:State): State =
        if(state.remainingAttempts <= 0) {
            channel.send(TrialExhausted)
            state
        } else {
            val job = launch {
                try {
                    download()
                    channel.send(FileDownloaded)
                } catch (t:Throwable){
                    channel.send(RetryDownload("exception: ${t.javaClass.simpleName}. message: ${t.message}"))
                }
            }
            state.copy(pendingJob = job)
        }

    private suspend fun download() {
        val file = fileToDownload.destination
        if(file.exists()){
            file.delete()
        }
        file.outputStream().use {
            client.downloadArtifact(fileToDownload.url).copyTo(it)
        }
    }

    private suspend fun checkMd5OfDownloadedFile(state:State): State {
        val job = launch {
            val file = fileToDownload.destination
            when {

                file.md5() == fileToDownload.md5 -> {
                    fileToDownload.onFileSaved()
                    channel.send(FileChecked)
                }

                file.delete() -> channel.send(Message.RetryDownload("unable verify md5 sum of downloaded file"))

                else -> throw IllegalStateException("UNABLE DELETE FILE $file")
            }

        }

        return state.copy(pendingJob = job)
    }

    init {
        become (beforeStart(State(attempts)))
    }

    companion object {
        const val DOWNLOADING_EXTENSION = "downloading"
        fun of(context: CoroutineContext,
               attempts: Int,
               fileToDownload: FileToDownload,
               client: IDdiClient) = Actor.actorOf(context) {
            DownloadManager(it,fileToDownload,attempts,client)
        }

        data class FileToDownload(val md5: String,
                                  val url: String,
                                  val folder: File){
            val destination = File(folder, "$md5.$DOWNLOADING_EXTENSION")
            fun onFileSaved() = destination.renameTo(File(folder, md5))
        }

        private data class State(
                val remainingAttempts:Int,
                val pendingJob: Job? = null,
                val result:Result = Result.PENDING,
                val listener: ActorRef?=null,
                val errors: List<String> = emptyList())

        sealed class Message {

            data class Start(val listener: ActorRef): Message()
            object Stop : Message()

            object FileDownloaded: Message()
            object FileChecked: Message()
            data class RetryDownload(val cause: String): Message()
            object TrialExhausted: Message()

            data class Success(val sender:ActorRef, val md5: String): Message()
            data class Info(val sender:ActorRef, val md5: String, val message:String): Message()
            data class Error(val sender:ActorRef, val md5: String, val message:String): Message()
        }

        enum class Result{
            ABORT, SUCCESS, ERROR, PENDING
        }
    }

}