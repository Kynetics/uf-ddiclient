package com.kynetics.updatefactory.ddiclient.core

import com.kynetics.updatefactory.ddiapiclient.api.model.DeplBaseResp
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplBaseResp.Depl.Appl.attempt
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplBaseResp.Depl.Appl.forced
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplFdbkReq
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplFdbkReq.Sts
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplFdbkReq.Sts.Exc
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplFdbkReq.Sts.Exc.closed
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplFdbkReq.Sts.Exc.proceeding
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplFdbkReq.Sts.Rslt
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplFdbkReq.Sts.Rslt.Fnsh
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplFdbkReq.Sts.Rslt.Fnsh.failure
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplFdbkReq.Sts.Rslt.Fnsh.none
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplFdbkReq.Sts.Rslt.Prgrs
import com.kynetics.updatefactory.ddiclient.core.ConnectionManager.Message.In.DeploymentFeedback
import com.kynetics.updatefactory.ddiclient.core.ConnectionManager.Message.Out.DeploymentInfo
import com.kynetics.updatefactory.ddiclient.core.DeploymentManager.Companion.Message.START_UPDATING
import com.kynetics.updatefactory.ddiclient.core.DeploymentManager.Companion.State.Download
import com.kynetics.updatefactory.ddiclient.core.DeploymentManager.Companion.State.Download.State.Status
import com.kynetics.updatefactory.ddiclient.core.DeploymentManager.Companion.State.Download.State.Status.*
import com.kynetics.updatefactory.ddiclient.core.DownloadManager.Companion.FileToDownload
import com.kynetics.updatefactory.ddiclient.core.DownloadManager.Companion.Message.*
import com.kynetics.updatefactory.ddiclient.core.api.DirectoryForArtifactsProvider
import com.kynetics.updatefactory.ddiclient.core.api.Updater
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ActorScope
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Instant
import kotlin.coroutines.CoroutineContext

@ObsoleteCoroutinesApi
class DeploymentManager
@UseExperimental(ObsoleteCoroutinesApi::class)
private constructor(private val scope: ActorScope<Any>,
                    private val parent: ActorRef,
                    private val connectionManager: ActorRef): Actor(scope) {

    private val registry: UpdaterRegistry = UpdateFactoryClientDefaultImpl.context!!.registry
    private val cdp: DirectoryForArtifactsProvider = UpdateFactoryClientDefaultImpl.context!!.directoryForArtifactsProvider

    private fun beforeStartReceive(): Receive = {
        msg ->
        when(msg) {

            is DeploymentInfo -> {
                val md5s = md5OfFilesToBeDownloaded(msg.info)
                if(md5s.isNotEmpty()){
                    val dms = createDownloadsMenagers(msg.info, md5s)
                    become(downloadingReceive(State(msg.info, dms)))
                    feedback(msg.info.id, proceeding,Prgrs(dms.size, 0), none,
                            "Start downloading ${dms.size} files")
                    dms.values.forEach{
                        it.downloader.send(Start(channel))
                    }
                }
            }
            else -> unhandled(msg)
        }
    }


    private fun downloadingReceive(state: State): Receive = { msg ->

        when(msg) {
            is DeploymentInfo -> {
                if(areEqualsApartHistoryMessages(msg.info, state.deplBaseResp)){
                    LOG.info("Skip unchanged deployment info")
                } else {
                    LOG.warn("to do, implement this flow")
                }
            }

            is Success -> processMessage(state, msg.md5, SUCCESS, "successfully downloaded file with md5 ${msg.md5}")

            is Info -> LOG.info(msg.toString())

            is Error -> processMessage(state, msg.md5, ERROR, "failed downloading file with md5 ${msg.md5} due to ${msg.message}", msg.message)

            else -> unhandled(msg)
        }


    }

    private fun pathCalculator(id: String):(artifact: Updater.SwModule.Artifact) -> String {
        return { artifact ->
            File(cdp.directoryForArtifacts(id), artifact.hashes.md5).absolutePath
        }
    }

    private fun updatingReceive(state: State): Receive = { msg ->
        when(msg) {

            is DeploymentInfo -> {
                if(areEqualsApartHistoryMessages(msg.info, state.deplBaseResp)){
                    LOG.info("Skip unchanged deployment info")
                } else {
                    LOG.warn("to do, implement this flow")
                }
            }

            is START_UPDATING -> {
                LOG.info("START UPDATING!!!")
                registry.allUpdatersWithSwModulesOrderedForPriority(state.deplBaseResp.deployment.chunks)
                        .forEach{
                            it.updater.apply(it.softwareModules.map { swModule ->
                                convert(swModule, pathCalculator(state.deplBaseResp.id)) }.toSet())
                        }
            }

            else -> unhandled(msg)
        }
    }

    private fun convert (swModule: Updater.SwModule, pathCalculator: (Updater.SwModule.Artifact) -> String): Updater.SwModuleWithPath =
            Updater.SwModuleWithPath(
                    swModule.metadata?.map { Updater.SwModuleWithPath.Metadata(it.key, it.value) }?.toSet(),
                    swModule.type,
                    swModule.name,
                    swModule.version,
                    swModule.artifacts.map
                    { Updater.SwModuleWithPath.Artifact(it.filename,it.hashes,it.size, pathCalculator(it))}.toSet()
            )

    private suspend fun processMessage(state:State, md5:String, status: Status, message: String, errorMsg:String?=null) {
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
                val messages = newState.downloads.filter { it.value.state.status == ERROR }.map { e ->
                    "the download of the file with md5 ${e.key} failed due to error(s): ${e.value.state.messages.joinToString(prefix = "\n", separator = "\n")}"
                }
                feedback(state.deplBaseResp.id, closed, progress, failure, "successfully downloaded file with md5 $md5", *messages.toTypedArray())
                newState.downloads.values.forEach{it.downloader.close()}
                become(downloadingReceive(newState.copy(downloads = emptyMap())))
            }
            else -> {
                become(updatingReceive(newState))
                parent.send(AuthorizationManager.Companion.Message.AskUpdateAuthorization)
            }
        }
    }




    private suspend fun feedback(id: String, execution: Exc, progress: Prgrs, finished: Fnsh, vararg messages: String) {
        val deplFdbkReq = DeplFdbkReq(id, Instant.now().toString(),Sts(
            execution,
            Rslt(
                finished,
                progress),
            messages.toList()
        ))
        connectionManager.send(DeploymentFeedback(deplFdbkReq))
    }

    private fun md5OfFilesToBeDownloaded(dbr: DeplBaseResp): Set<String> {
        val d = dbr.deployment.download
        return if(d==attempt || d == forced){
            registry.allRequiredArtifactsFor(dbr.deployment.chunks).map { it.md5 }.toSet()
        } else emptySet()
    }

    private fun createDownloadsMenagers(dbr: DeplBaseResp, md5s: Set<String>): Map<String, Download> {
        val wd = cdp.directoryForArtifacts(dbr.id)
        if (!wd.exists()) {
            wd.mkdir()
        }
        return dbr.deployment.chunks.flatMap { it.artifacts }.filter { md5s.contains(it.hashes.md5) }.map { at ->
            val md5 = at.hashes.md5
            val ftd = FileToDownload(md5, at._links.download_http.href, wd)
            val dm = DownloadManager.of(scope.coroutineContext, this.channel, 3, ftd)
            Pair(md5, Download(dm))
        }.toMap()
    }



    private fun areEqualsApartHistoryMessages(deplBaseResp1: DeplBaseResp, deplBaseResp2: DeplBaseResp): Boolean {
        fun pruneMessages(dbr: DeplBaseResp) = dbr.copy(actionHistory = dbr.actionHistory?.copy(messages = emptyList()))
        return pruneMessages(deplBaseResp1) == pruneMessages(deplBaseResp2)
    }

    init {
        become(beforeStartReceive())
    }

    companion object {
        fun of(context: CoroutineContext,
               parent: ActorRef,
               connectionManager: ActorRef) = Actor.actorOf(context = context, parent = parent){
            DeploymentManager(it, parent, connectionManager)
        }

        data class State(
                val deplBaseResp: DeplBaseResp,
                val downloads: Map<String,Download> = emptyMap()){
            data class Download(val downloader:ActorRef, val state:State=State()){
                data class State(val status:Status=Status.RUNNING, val messages: List<String> = emptyList()){
                    enum class Status{ SUCCESS, ERROR, RUNNING}
                }
            }
        }

        sealed class Message {
            object START_UPDATING: Message()
        }

        private val LOG = LoggerFactory.getLogger(DeploymentManager::class.java)

    }
}