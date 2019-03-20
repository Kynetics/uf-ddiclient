package com.kynetics.updatefactory.ddiclient.core.actors

import com.kynetics.updatefactory.ddiapiclient.api.model.CfgDataReq
import com.kynetics.updatefactory.ddiclient.core.actors.ConnectionManager.Companion.Message.In
import com.kynetics.updatefactory.ddiclient.core.actors.ConnectionManager.Companion.Message.Out.*
import com.kynetics.updatefactory.ddiclient.core.actors.ConnectionManager.Companion.Message.Out.Err.ErrMsg
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.runBlocking
import com.kynetics.updatefactory.ddiclient.core.actors.DeploymentManager.Companion.Message.DownloadFailed
import com.kynetics.updatefactory.ddiclient.core.actors.DeploymentManager.Companion.Message.*

//TODO set frequent ping during deployment
@UseExperimental(ObsoleteCoroutinesApi::class)
class ActionManager
private constructor(scope: ActorScope): AbstractActor(scope) {

    private val configDataProvider = coroutineContext[UFClientContext]!!.configDataProvider
    private val connectionManager  = coroutineContext[CMActor]!!.ref

    private fun defaultReceive(state: State):Receive = { msg ->
        when {

            msg is ConfigDataRequired -> {
                val map = configDataProvider.configData()

                if(map.isNotEmpty()){
                    val cdr = CfgDataReq.of(map, CfgDataReq.Mod.merge)
                    connectionManager.send(In.ConfigDataFeedback(cdr))
                } else {
                    LOG.info("Config dara required ignored because of map is empty")
                }
            }

            msg is DeploymentInfo && state.alreadyProcessing(msg) -> LOG.info("Skip unchanged deployment info")

            msg is DeploymentInfo && state.inDeployment(msg.info.id) ->  child("deploymentManager")!!.send(msg)

            msg is DeploymentInfo && state.inDeployment -> LOG.info("HANGED DEPLOYMENT ID ???")

            msg is DeploymentInfo -> {
                val deploymentManager = actorOf("deploymentManager"){ DeploymentManager.of(it) }
                become(defaultReceive(state.copy(deployment = msg)))
                deploymentManager.send(msg)
            }

            msg is DownloadFailed -> {
                LOG.warn("DownloadFailed. Not yet implemented")
                become(defaultReceive(state.copy(deployment = null)))
                child("deploymentManager")!!.close()

            }

            msg is UpdateFailed -> {
                LOG.warn("UpdateFailed. Not yet implemented")
                become(defaultReceive(state.copy(deployment = null)))
                child("deploymentManager")!!.close()

            }

            msg is UpdateFinished -> {
                LOG.info("UpdateFinished. Not yet implemented")
                become(defaultReceive(state.copy(deployment = null)))
                child("deploymentManager")!!.close()
            }

            msg is DeploymentCancelInfo -> {
                LOG.warn("DeploymentCancelInfo. Not yet implemented")
            }

            msg is ErrMsg -> {
                LOG.warn("ErrMsg. Not yet implemented")
            }

            else -> unhandled(msg)
        }
    }

    init {
        become(defaultReceive(State()))
        runBlocking { connectionManager.send(In.Register(channel)) }
    }

    companion object {
        fun of(scope: ActorScope) = ActionManager(scope)

        data class State(val deployment: DeploymentInfo? = null) {
            val inDeployment = deployment != null
            fun inDeployment(id: String) = inDeployment && deployment!!.info.id == id
            fun alreadyProcessing(msg: DeploymentInfo) = inDeployment && deployment!!.equalsApartHistory(msg)
        }
    }

}