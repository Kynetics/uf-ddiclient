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
import kotlin.coroutines.CoroutineContext

class ActionManager @UseExperimental(ObsoleteCoroutinesApi::class)
private constructor(scope: ActorScope<Any>,
                    private val connectionManager: ActorRef,
                    private val registry: UpdaterRegistry,
                    private val configDataProvider: ConfigDataProvider,
                    private val ddiClient: IDdiClient): Actor(scope) {

    private fun defaultReceive(state: State):Receive = { msg ->
        when(msg) {

            is ConfigDataRequired -> {
               connectionManager.send(CfgDataReq.of(configDataProvider.configData(), CfgDataReq.Mod.merge))
            }

            is DeploymentInfo -> {
                if(state.inDeployment && state.deploymentId!! == msg.info.id){
                    state.deploymentManager!!.send(msg)
                } else if(state.inDeployment) {
                    println("HANGED DEPLOYMENT ID ???")
                } else {
                    val deploymentManager = DeploymentManager.of(
                            GlobalScope.coroutineContext,
                            this.channel,
                            connectionManager,
                            registry,
                            ddiClient)
                    become(defaultReceive(state.copy(deployment = Deplyment(msg.info.id,deploymentManager))))
                    deploymentManager.send(msg)
                }
            }

            is DeploymentCancelInfo -> {
                println(msg)
            }

            is ErrMsg -> {
                println(msg)
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

        data class State(val deployment: Deplyment? = null) {
            data class Deplyment(val id:String, val manager: ActorRef)
            val inDeployment = deployment != null
            val deploymentId = deployment?.id
            val deploymentManager = deployment?.manager
        }
    }
}