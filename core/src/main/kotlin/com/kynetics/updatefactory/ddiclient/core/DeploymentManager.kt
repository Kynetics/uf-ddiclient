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

class DeploymentManager
@UseExperimental(ObsoleteCoroutinesApi::class)
private constructor(scope: ActorScope<Any>,
                    private val actionManager: ActorRef,
                    private val connectionManager: ActorRef,
                    private val registry: UpdaterRegistry,
                    private val ddiClient: IDdiClient): Actor(scope) {

    private fun beforeStartReceive(): Receive = { msg ->
        when(msg) {
            is DeploymentInfo -> {
                become(runningReceive(State(msg.info)))
                startWorkflow(msg.info)
            }
            else -> unhandled(msg)
        }
    }


    private fun runningReceive(state: State): Receive = { msg ->
        when(msg) {
            is DeploymentInfo -> {
                println("DeploymentInfo in running Receive")
            }
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

    private suspend fun startWorkflow(dbr: DeplBaseResp) {
        val dnld = dbr.deployment.download
        if(dnld == attempt || dnld == forced) {
            val fileCount = startDownload(dbr)
            if(fileCount > 0) {
                feedback(dbr.id, proceeding,Prgrs(fileCount, 0), none,
                        "Start downloading $fileCount files")
            }
        }
    }

    private suspend fun startDownload(dbr: DeplBaseResp): Int {
        val md5s = registry.allRequiredArtifactsFor(dbr.deployment.chunks).map { it.md5 }
        val fileCount = md5s.size
//        if(fileCount > 0){
//            val tmpDir = Paths.get(dbr.id)
//            if(!Files.exists(tmpDir)){
//                Files.createDirectory(tmpDir)
//            }
//            val artefacts = dbr.deployment.chunks.flatMap { it.artifacts }.filter { md5s.contains(it.hashes.md5) }
//            val downloads = artefacts.map { a ->
//                ddiClient.downloadArtifact(a._links.download.href).use { i ->
//                    Files.newOutputStream(Paths.get(tmpDir, "${a.hashes.md5}.dwnld")).use { o ->
//                        async {
//                            i.copyTo(o)
//                            a.filename
//                        }
//                    }
//                }
//            }
//            var i=1
//            while(downloads.count{it.isActive} > 0) {
//                val result = select<String> {
//                    downloads.forEach{it.onAwait}
//                }
//                feedback(dbr.id, proceeding, Prgrs(fileCount, i++), none,
//                        "downloaded file $result")
//
//            }
//            val result = select<String> {
//                downloads.forEach { def ->
//                    def.onAwait { filename ->
//                        feedback(dbr.id, proceeding, Prgrs(fileCount, ai.getAndIncrement()), none,
//                                "downloaded file $filename")
//                        filename
//                    }
//                }
//            }
//            resu
//        }
        return fileCount
    }



//    private fun areEqualsApartHistoryMessages(deplBaseResp1: DeplBaseResp, deplBaseResp2: DeplBaseResp): Boolean {
//        fun pruneMessages(dbr: DeplBaseResp) = dbr.copy(actionHistory = dbr.actionHistory?.copy(messages = emptyList()))
//        return pruneMessages(deplBaseResp1) == pruneMessages(deplBaseResp2)
//    }

    init {
        become(beforeStartReceive())
    }

    companion object {
        fun of(context: CoroutineContext,
               actionManager: ActorRef,
               connectionManager: ActorRef,
               registry: UpdaterRegistry,
               ddiClient: IDdiClient) = Actor.actorOf(context){
            DeploymentManager(it, actionManager, connectionManager, registry, ddiClient)
        }

        data class State(val deplBaseResp: DeplBaseResp)
    }
}