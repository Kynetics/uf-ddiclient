package com.kynetics.updatefactory.ddiclient.core

import com.kynetics.updatefactory.ddiapiclient.api.IDdiClient
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ActorScope
import kotlinx.coroutines.launch
import java.io.File
import java.security.MessageDigest
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Job
import java.security.DigestInputStream

class DownloadManager
@UseExperimental(ObsoleteCoroutinesApi::class)
private constructor(scope: ActorScope<Any>,
                    private val deploymentManager: ActorRef,
                    private val fileToDownload: FileToDownload,
                    private val attempts: Int,
                    private val client: IDdiClient,
                    private val id: String): Actor(scope) {

    private fun beforeStart(state:State): Receive = { msg ->

        when(msg) {

            is Message.Start -> startDownload(state)

            is Message.Stop  -> become(finished(state.copy(result = Result.ABORT)))

            else             -> unhandled(msg)

        }

    }

    private fun downloading(state:State): Receive = { msg ->

        when(msg) {

            is Message.FileSaved -> become(downloading(onFileSaved(state)))

            is Message.Retry     -> startDownload(state)

            is Message.Stop      -> {
                state.pendingJob?.cancel()
                become(finished(state.copy(result = Result.ABORT)))
            }

            is Message.Success   -> become(finished(state.copy(result = Result.SUCCESS)))

            is Message.Error     -> become(finished(state.copy(result = Result.ERROR)))

            else                 -> unhandled(msg)

        }

    }

    private fun finished(state:State): Receive = { msg ->
        when(msg){

            is Message.Stop,
            Message.Retry,
            Message.Success,
            Message.Error,
            Message.FileSaved -> {}

            is Message.Ping   -> notifyDownloadManager(state)

            else              -> unhandled(msg)
        }

    }

    private suspend fun startDownload(state:State){

        if(state.remainingAttempts < 1){
            become(finished(state.copy(result = Result.ERROR)))
            deploymentManager.send(Message.Error)
            return
        }

        become(downloading(state.copy(remainingAttempts = state.remainingAttempts-1)))

        val job = launch {
            if(!fileToDownload.destination.exists()){
                fileToDownload.destination.outputStream().use {
                    client.downloadArtifact(fileToDownload.url).copyTo(it)
                }
            }
            channel.send(Message.FileSaved)
        }

        become(downloading(state.copy(pendingJob = job)))

    }

    private suspend fun onFileSaved(state:State):State{
        val job = launch {

            when {

                fileToDownload.destination.md5()
                        == fileToDownload.md5              -> {
                    fileToDownload.onFileSaved()
                    deploymentManager.send(Message.Success)
                    channel.send(Message.Success)
                }

                fileToDownload.destination.delete()        -> channel.send(Message.Retry)

                else                                       -> channel.send(Message.Error)
            }

        }

        return state.copy(pendingJob = job)
    }

    private suspend fun notifyDownloadManager(state:State){

        when(state.result){

            Result.SUCCESS -> deploymentManager.send(Message.Success)

            Result.ERROR   -> deploymentManager.send(Message.Error)

            Result.PENDING -> {}

            Result.ABORT   -> deploymentManager.send(Message.Error)
        }

    }

    private fun File.md5():String{
        val md = MessageDigest.getInstance("MD5")
        val sb = StringBuilder()

        inputStream().use { fis ->
            val digestInputStream = DigestInputStream(fis, md)
            val buffer = ByteArray(4096)
            while (digestInputStream.read(buffer) > -1){}
            digestInputStream.close()
            digestInputStream.messageDigest
                    .digest()
                    .forEach {
                        sb.append(String.format("%02X", it))
                    }
        }

        return sb.toString().toLowerCase()
    }


    init {
        become { beforeStart(State(attempts)) }
    }

    companion object {

        const val DOWNLOADING_EXTENSION = "downloading"

        fun of(context: CoroutineContext,
               deploymentManager: ActorRef,
               attempts: Int,
               fileToDownload: FileToDownload,
               client: IDdiClient,
               id: String) = Actor.actorOf(context) {
            DownloadManager(it,deploymentManager,fileToDownload,attempts,client, id)
        }

        data class FileToDownload(val md5: String,
                                  val url: String,
                                  val folder: File){
            val destination = File(folder, "$md5.$DOWNLOADING_EXTENSION")
            fun onFileSaved() = destination.renameTo(File(folder, md5))
        }

        private data class State(val remainingAttempts:Int, val pendingJob: Job? = null, val result:Result = Result.PENDING)

        sealed class Message {

            object Start: Message()
            object Stop : Message()
            object FileSaved: Message()
            object Retry: Message()
            object Success: Message()
            object Error: Message()
            object Ping: Message()

        }

        enum class Result{
            ABORT, SUCCESS, ERROR, PENDING
        }
    }

}