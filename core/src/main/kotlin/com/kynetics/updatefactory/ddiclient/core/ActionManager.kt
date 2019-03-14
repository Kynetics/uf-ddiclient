package com.kynetics.updatefactory.ddiclient.core

import com.kynetics.updatefactory.ddiapiclient.api.IDdiClient
import com.kynetics.updatefactory.ddiapiclient.api.model.CfgDataReq
import com.kynetics.updatefactory.ddiclient.core.ActionManager.Companion.State.Deplyment
import com.kynetics.updatefactory.ddiclient.core.ConnectionManager.Companion.Message.In
import com.kynetics.updatefactory.ddiclient.core.ConnectionManager.Companion.Message.Out.*
import com.kynetics.updatefactory.ddiclient.core.ConnectionManager.Companion.Message.Out.Err.ErrMsg
import com.kynetics.updatefactory.ddiclient.core.api.ConfigDataProvider
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ActorScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext

@ObsoleteCoroutinesApi
class ActionManager @UseExperimental(ObsoleteCoroutinesApi::class)
private constructor(val scope: ActorScope<Any>,
                    private val connectionManager: ActorRef,
                    private val registry: UpdaterRegistry,
                    private val configDataProvider: ConfigDataProvider,
                    private val ddiClient: IDdiClient): Actor(scope) {

    private fun defaultReceive(state: State):Receive = { msg ->
        when(msg) {

            is ConfigDataRequired -> {
                val map = configDataProvider.configData()+("key" to "")

                if(!map.isEmpty()){
                    val cdr = CfgDataReq.of(map, CfgDataReq.Mod.merge)
                    connectionManager.send(In.ConfigDataFeedback(cdr))
                }
            }

            is DeploymentInfo -> {
                if(state.inDeployment && state.deploymentId!! == msg.info.id){
                    state.deploymentManager!!.send(msg)
                } else if(state.inDeployment) {
                    if(LOG.isInfoEnabled){
                        LOG.info("HANGED DEPLOYMENT ID ???")
                    }
                } else {
                    val deploymentManager = DeploymentManager.of(
                            scope.coroutineContext,
                            this.channel,
                            connectionManager,
                            registry,
                            configDataProvider,
                            ddiClient)
                    become(defaultReceive(state.copy(deployment = Deplyment(msg.info.id,deploymentManager))))
                    deploymentManager.send(msg)
                }
            }

            is DeploymentCancelInfo -> {
                if(LOG.isInfoEnabled){
                    LOG.info(msg.toString())
                }
            }

            is ErrMsg -> {
                if(LOG.isInfoEnabled){
                    LOG.info("HANGED DEPLOYMENT ID ???")
                }
            }

            else -> unhandled(msg)

        }
    }

    init {
        become(defaultReceive(State()))
        launch { connectionManager.send(In.Register(channel)) }
    }

    companion object {
        fun of(context: CoroutineContext,
               connectionManager: ActorRef,
               registry: UpdaterRegistry,
               configDataProvider: ConfigDataProvider,
               ddiClient: IDdiClient) = Actor.actorOf(context) {
            ActionManager(it, connectionManager, registry, configDataProvider, ddiClient)
        }

        private val LOG = LoggerFactory.getLogger(ActionManager::class.java)

        data class State(val deployment: Deplyment? = null) {
            data class Deplyment(val id:String, val manager: ActorRef)
            val inDeployment = deployment != null
            val deploymentId = deployment?.id
            val deploymentManager = deployment?.manager
        }
    }

}