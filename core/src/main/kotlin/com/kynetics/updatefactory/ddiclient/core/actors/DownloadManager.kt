package com.kynetics.updatefactory.ddiclient.core.actors

import com.kynetics.updatefactory.ddiapiclient.api.model.DeplBaseResp
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplBaseResp.Depl.Appl.attempt
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplBaseResp.Depl.Appl.forced
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplFdbkReq
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplFdbkReq.Sts
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplFdbkReq.Sts.Exc
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplFdbkReq.Sts.Exc.proceeding
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplFdbkReq.Sts.Rslt
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplFdbkReq.Sts.Rslt.Fnsh
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplFdbkReq.Sts.Rslt.Fnsh.failure
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplFdbkReq.Sts.Rslt.Fnsh.none
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplFdbkReq.Sts.Rslt.Prgrs
import com.kynetics.updatefactory.ddiclient.core.actors.ConnectionManager.Companion.Message.In.DeploymentFeedback
import com.kynetics.updatefactory.ddiclient.core.actors.ConnectionManager.Companion.Message.Out.DeploymentInfo
import com.kynetics.updatefactory.ddiclient.core.actors.DeploymentManager.Companion.Message.DownloadFailed
import com.kynetics.updatefactory.ddiclient.core.actors.DeploymentManager.Companion.Message.DownloadFinished
import com.kynetics.updatefactory.ddiclient.core.actors.DownloadManager.Companion.State.Download
import com.kynetics.updatefactory.ddiclient.core.actors.DownloadManager.Companion.State.Download.State.Status
import com.kynetics.updatefactory.ddiclient.core.actors.DownloadManager.Companion.State.Download.State.Status.*
import com.kynetics.updatefactory.ddiclient.core.actors.FileDownloader.Companion.FileToDownload
import com.kynetics.updatefactory.ddiclient.core.actors.FileDownloader.Companion.Message.*
import kotlinx.coroutines.ObsoleteCoroutinesApi
import java.time.Instant

@UseExperimental(ObsoleteCoroutinesApi::class)
class DownloadManager
private constructor(scope: ActorScope): AbstractActor(scope) {

    private val registry = coroutineContext[UFClientContext]!!.registry
    private val dfap      = coroutineContext[UFClientContext]!!.directoryForArtifactsProvider
    private val connectionManager = coroutineContext[CMActor]!!.ref

    private fun beforeStartReceive(): Receive = { msg ->
        when(msg) {

            is DeploymentInfo -> {
                val md5s = md5OfFilesToBeDownloaded(msg.info)
                if(md5s.isNotEmpty()){
                    val dms = createDownloadsMenagers(msg.info, md5s)
                    become(downloadingReceive(State(msg.info, dms)))
                    feedback(msg.info.id, proceeding,Prgrs(dms.size, 0), none,
                            "Start downloading ${dms.size} files")
                    dms.values.forEach{
                        it.downloader.send(Start)
                    }
                }
            }
            else -> unhandled(msg)
        }
    }

    private fun downloadingReceive(state: State): Receive = { msg ->
        when(msg) {

            is Success -> {
                processMessage(state, msg.md5, SUCCESS, "successfully downloaded file with md5 ${msg.md5}")
            }

            is Info -> LOG.info(msg.toString())

            is Error -> {
                processMessage(state, msg.md5, ERROR, "failed downloading file with md5 ${msg.md5} due to ${msg.message}", msg.message)
            }

            else -> unhandled(msg)
        }
    }

    private suspend fun processMessage(state: State, md5:String, status: Status, message: String, errorMsg:String?=null) {
        val download = state.downloads.getValue(md5)
        val newErrMessages = if(errorMsg == null) download.state.messages else download.state.messages + errorMsg
        val newDownload = download.copy(state = download.state.copy(status = status, messages = newErrMessages))
        val newState = state.copy(downloads = state.downloads + (md5 to newDownload))
        val downloads = newState.downloads.values
        val progress = Prgrs(
                downloads.size,
                downloads.count { it.state.status == SUCCESS })
        when {
            downloads.any { it.state.status == RUNNING } -> {
                feedback(state.deplBaseResp.id, proceeding, progress, none, message)
                become(downloadingReceive(newState))
            }
            downloads.any { it.state.status == ERROR } -> {
                feedback(state.deplBaseResp.id, proceeding, progress, failure, message)
                newState.downloads.values.forEach{it.downloader.close()}
                parent!!.send(DownloadFailed)
            }
            else -> {
                feedback(state.deplBaseResp.id, proceeding, progress, none, "successfully downloaded all files")
                newState.downloads.values.forEach{it.downloader.close()}
                parent!!.send(DownloadFinished)
                channel.close()
            }
        }
    }

    private suspend fun feedback(id: String, execution: Exc, progress: Prgrs, finished: Fnsh, vararg messages: String) {
        val deplFdbkReq = DeplFdbkReq.newInstance(id, execution, progress, finished, *messages)
        connectionManager.send(DeploymentFeedback(deplFdbkReq))
    }

    private fun md5OfFilesToBeDownloaded(dbr: DeplBaseResp): Set<String> = when(dbr.deployment.download){
        attempt, forced -> registry.allRequiredArtifactsFor(dbr.deployment.chunks).map { it.md5 }.toSet()
        else            -> emptySet()
    }

    private fun createDownloadsMenagers(dbr: DeplBaseResp, md5s: Set<String>): Map<String, Download> {
        val wd = dfap.directoryForArtifacts(dbr.id)
        if (!wd.exists()) {
            wd.mkdir()
        }
        return dbr.deployment.chunks.flatMap { it.artifacts }.filter { md5s.contains(it.hashes.md5) }.map { at ->
            val md5 = at.hashes.md5
            val ftd = FileToDownload(md5, at._links.download_http.href, wd)
            val dm = actorOf(childName(md5)){
                FileDownloader.of(it, 3, ftd)
            }
            Pair(md5, Download(dm))
        }.toMap()
    }

    private fun childName(md5: String) = "fileDownloader_for_$md5"

    init {
        become(beforeStartReceive())
    }

    companion object {
        fun of(scope: ActorScope) = DownloadManager(scope)


        data class State(
                val deplBaseResp: DeplBaseResp,
                val downloads: Map<String,Download> = emptyMap()){
            data class Download(val downloader:ActorRef, val state:State=State()){
                data class State(val status:Status=Status.RUNNING, val messages: List<String> = emptyList()){
                    enum class Status{ SUCCESS, ERROR, RUNNING}
                }
            }
        }
    }
}