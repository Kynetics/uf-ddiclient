package com.kynetics.updatefactory.ddiclient.core.actors

import com.kynetics.updatefactory.ddiapiclient.api.model.DeplFdbkReq
import com.kynetics.updatefactory.ddiclient.core.actors.ConnectionManager.Companion.Message.Out.DeploymentInfo
import com.kynetics.updatefactory.ddiclient.core.api.Updater
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.runBlocking
import java.io.File
import com.kynetics.updatefactory.ddiclient.core.actors.ConnectionManager.Companion.Message.In.DeploymentFeedback
import com.kynetics.updatefactory.ddiclient.core.api.EventListener

@UseExperimental(ObsoleteCoroutinesApi::class)
class UpdateManager
private constructor(scope: ActorScope): AbstractActor(scope) {

    private val registry = coroutineContext[UFClientContext]!!.registry
    private val dfap      = coroutineContext[UFClientContext]!!.directoryForArtifactsProvider
    private val connectionManager = coroutineContext[CMActor]!!.ref
    private val notificationManager = coroutineContext[NMActor]!!.ref

    private fun beforeStartReceive(): Receive = { msg ->
        when(msg) {

            is DeploymentInfo -> {
                LOG.info("START UPDATING!!!")
                notificationManager.send(EventListener.Event.StartUpdate)
                val updaters = registry.allUpdatersWithSwModulesOrderedForPriority(msg.info.deployment.chunks)
                val lastSuccessUpdaterPairedWithIndex = updaters
                        .mapIndexed{index, u -> index to u }
                        .takeWhile { (index, it) ->
                            it.updater.apply(it.softwareModules.map { swModule ->
                                convert(swModule, pathCalculator(msg.info.id)) }.toSet(), object: Updater.Messanger{
                                override fun sendMessageToServer(msgStr: String) {
                                    runBlocking {
                                        sendFeedback(msg.info.id,
                                                DeplFdbkReq.Sts.Exc.proceeding,
                                                DeplFdbkReq.Sts.Rslt.Prgrs(updaters.size, index),
                                                DeplFdbkReq.Sts.Rslt.Fnsh.none,
                                                msgStr)
                                    }
                                }
                            })
                        }.last()

                if(lastSuccessUpdaterPairedWithIndex.first != updaters.size - 1){
                    LOG.warn("update ${lastSuccessUpdaterPairedWithIndex.first} failed!")
                    parent!!.send(DeploymentManager.Companion.Message.UpdateFailed)
                    sendFeedback(msg.info.id,
                            DeplFdbkReq.Sts.Exc.closed,
                            DeplFdbkReq.Sts.Rslt.Prgrs(updaters.size, lastSuccessUpdaterPairedWithIndex.first),
                            DeplFdbkReq.Sts.Rslt.Fnsh.failure,
                            "Update failed")
                    notificationManager.send(EventListener.Event.UpdateFinished(successApply = false, details = emptyList()))
                } else {
                    parent!!.send(DeploymentManager.Companion.Message.UpdateFinished)
                    sendFeedback(msg.info.id,
                            DeplFdbkReq.Sts.Exc.closed,
                            DeplFdbkReq.Sts.Rslt.Prgrs(updaters.size, updaters.size),
                            DeplFdbkReq.Sts.Rslt.Fnsh.success,
                            "Update finished")
                    notificationManager.send(EventListener.Event.UpdateFinished(successApply = true, details = emptyList()))
                }
            }

            else -> unhandled(msg)
        }

    }

    private suspend fun sendFeedback(id: String,
                                     execution: DeplFdbkReq.Sts.Exc,
                                     progress: DeplFdbkReq.Sts.Rslt.Prgrs,
                                     finished: DeplFdbkReq.Sts.Rslt.Fnsh,
                                     vararg messages: String){
        val request = DeplFdbkReq.newInstance(id,execution, progress, finished, *messages)
        connectionManager.send(DeploymentFeedback(request))
    }

    private fun pathCalculator(id: String):(artifact: Updater.SwModule.Artifact) -> String {
        return { artifact ->
            File(dfap.directoryForArtifacts(id), artifact.hashes.md5).absolutePath
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

    init {
        become(beforeStartReceive())
    }

    companion object {
        fun of(scope: ActorScope) = UpdateManager(scope)
    }
}

