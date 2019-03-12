package com.kynetics.updatefactory.ddiclient.core

import com.kynetics.updatefactory.ddiapiclient.api.IDdiClient
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplBaseResp
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplBaseResp.Depl.Appl.attempt
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplBaseResp.Depl.Appl.forced
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplFdbkReq
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplFdbkReq.Sts
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplFdbkReq.Sts.Exc
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplFdbkReq.Sts.Exc.proceeding
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplFdbkReq.Sts.Rslt
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplFdbkReq.Sts.Rslt.Fnsh
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplFdbkReq.Sts.Rslt.Fnsh.none
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplFdbkReq.Sts.Rslt.Prgrs
import com.kynetics.updatefactory.ddiclient.core.ConnectionManager.Companion.Message.In.DeploymentFeedback
import com.kynetics.updatefactory.ddiclient.core.ConnectionManager.Companion.Message.Out.DeploymentInfo
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ActorScope
import java.time.Instant
import kotlin.coroutines.CoroutineContext
import com.kynetics.updatefactory.ddiclient.core.DownloadManager.Companion.FileToDownload
import com.kynetics.updatefactory.ddiclient.core.api.ConfigDataProvider
import com.kynetics.updatefactory.ddiclient.core.DownloadManager.Companion.Message.*

@ObsoleteCoroutinesApi
class DeploymentManager
@UseExperimental(ObsoleteCoroutinesApi::class)
private constructor(val scope: ActorScope<Any>,
                    private val actionManager: ActorRef,
                    private val connectionManager: ActorRef,
                    private val registry: UpdaterRegistry,
                    private val cdp: ConfigDataProvider,
                    private val ddiClient: IDdiClient): Actor(scope) {

    private fun beforeStartReceive(): Receive = { msg ->
        when(msg) {

            is DeploymentInfo -> {
                val md5s = md5OffilesToBeDownloaded(msg.info)
                if(md5s.isNotEmpty()){
                    val dms = createDownloadsMenagers(msg.info, md5s)
                    become(runningReceive(State(msg.info, dms)))
                    feedback(msg.info.id, proceeding,Prgrs(dms.size, 0), none,
                            "Start downloading ${dms.size} files")
                    dms.values.forEach{
                        it.send(Start(channel))
                    }
                }
            }
            else -> unhandled(msg)
        }
    }


    private fun runningReceive(state: State): Receive = { msg ->
        when(msg) {

            is DeploymentInfo -> {
                if(areEqualsApartHistoryMessages(msg.info, state.deplBaseResp)){
                    println("Skip unchanged deployment info")
                } else {
                    println("to do, implement this flow")
                }
            }

            is Success -> println(msg)

            is Info -> println(msg)

            is Error -> println(msg)

            else -> unhandled(msg)
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

    private fun md5OffilesToBeDownloaded(dbr: DeplBaseResp): Set<String> {
        val d = dbr.deployment.download
        return if(d==attempt || d == forced){
            registry.allRequiredArtifactsFor(dbr.deployment.chunks).map { it.md5 }.toSet()
        } else emptySet()
    }

    private fun createDownloadsMenagers(dbr: DeplBaseResp, md5s: Set<String>): Map<String, ActorRef> {
        val wd = cdp.directoryPathForArtifacts(dbr.id).toFile()
        if (!wd.exists()) {
            wd.mkdir()
        }
        return dbr.deployment.chunks.flatMap { it.artifacts }.filter { md5s.contains(it.hashes.md5) }.map { at ->
            val md5 = at.hashes.md5
            val ftd = FileToDownload(md5, at._links.download_http.href, wd)
            val dm = DownloadManager.of(scope.coroutineContext, 3, ftd, ddiClient)
            Pair(md5, dm)
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
               actionManager: ActorRef,
               connectionManager: ActorRef,
               registry: UpdaterRegistry,
               cdp: ConfigDataProvider,
               ddiClient: IDdiClient) = Actor.actorOf(context){
            DeploymentManager(it, actionManager, connectionManager, registry, cdp, ddiClient)
        }

        data class State(
                val deplBaseResp: DeplBaseResp,
                val downloads: Map<String,ActorRef> = emptyMap())
    }
}