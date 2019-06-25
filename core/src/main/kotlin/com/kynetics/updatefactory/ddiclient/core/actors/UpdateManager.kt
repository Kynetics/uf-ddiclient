package com.kynetics.updatefactory.ddiclient.core.actors

import com.kynetics.updatefactory.ddiapiclient.api.model.DeplFdbkReq
import com.kynetics.updatefactory.ddiclient.core.actors.ConnectionManager.Companion.Message.Out.DeploymentInfo
import com.kynetics.updatefactory.ddiclient.core.api.Updater
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.runBlocking
import com.kynetics.updatefactory.ddiclient.core.actors.ConnectionManager.Companion.Message.In.DeploymentFeedback
import com.kynetics.updatefactory.ddiclient.core.api.MessageListener

@UseExperimental(ObsoleteCoroutinesApi::class)
class UpdateManager
private constructor(scope: ActorScope): AbstractActor(scope) {

    private val registry = coroutineContext[UFClientContext]!!.registry
    private val pathResolver      = coroutineContext[UFClientContext]!!.pathResolver
    private val connectionManager = coroutineContext[CMActor]!!.ref
    private val notificationManager = coroutineContext[NMActor]!!.ref

    private fun beforeStartReceive(): Receive = { msg ->
        when(msg) {

            is DeploymentInfo -> {
                LOG.info("START UPDATING!!!")
                notificationManager.send(MessageListener.Message.State.Updating)
                val updaters = registry.allUpdatersWithSwModulesOrderedForPriority(msg.info.deployment.chunks)
                val details = mutableListOf("Details:")
                val updaterError = updaters
                        .mapIndexed{index, u -> index to u }
                        .dropWhile { (index, it) ->
                            val updateResult = it.updater.apply(it.softwareModules.map { swModule ->
                                convert(swModule, pathResolver.fromArtifact(msg.info.id)) }.toSet(), object: Updater.Messenger{
                                override fun sendMessageToServer(vararg msgStr: String) {
                                    runBlocking {
                                        sendFeedback(msg.info.id,
                                                DeplFdbkReq.Sts.Exc.proceeding,
                                                DeplFdbkReq.Sts.Rslt.Prgrs(updaters.size, index),
                                                DeplFdbkReq.Sts.Rslt.Fnsh.none,
                                                *msgStr)
                                    }
                                }
                            })
                            details.add("Feedback updater named ${it.updater.javaClass.simpleName}")
                            details.addAll(updateResult.details)
                            updateResult.success
                        }

                if(updaterError.isNotEmpty()){
                    LOG.warn("update ${updaterError[0].first} failed!")
                    parent!!.send(DeploymentManager.Companion.Message.UpdateFailed)
                    sendFeedback(msg.info.id,
                            DeplFdbkReq.Sts.Exc.closed,
                            DeplFdbkReq.Sts.Rslt.Prgrs(updaters.size, updaterError[0].first),
                            DeplFdbkReq.Sts.Rslt.Fnsh.failure,
                            *details.toTypedArray())
                    notificationManager.send(MessageListener.Message.Event.UpdateFinished(successApply = false, details = details))
                } else {
                    parent!!.send(DeploymentManager.Companion.Message.UpdateFinished)
                    sendFeedback(msg.info.id,
                            DeplFdbkReq.Sts.Exc.closed,
                            DeplFdbkReq.Sts.Rslt.Prgrs(updaters.size, updaters.size),
                            DeplFdbkReq.Sts.Rslt.Fnsh.success,
                            *details.toTypedArray())
                    notificationManager.send(MessageListener.Message.Event.UpdateFinished(successApply = true, details = details))
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

