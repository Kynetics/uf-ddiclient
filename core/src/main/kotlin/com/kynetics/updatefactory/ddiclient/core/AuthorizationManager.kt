package com.kynetics.updatefactory.ddiclient.core

import com.kynetics.updatefactory.ddiapiclient.api.model.DeplBaseResp
import com.kynetics.updatefactory.ddiclient.core.ConnectionManager.Companion.Message.Out.DeploymentInfo
import com.kynetics.updatefactory.ddiclient.core.DeploymentManager.Companion.Message.START_UPDATING
import com.kynetics.updatefactory.ddiclient.core.api.AuthorizationRequest
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ActorScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext

@ObsoleteCoroutinesApi
class AuthorizationManager
@UseExperimental(ObsoleteCoroutinesApi::class)
private constructor(private val scope: ActorScope<Any>,
                    private val parent: ActorRef,
                    private val connectionManager: ActorRef): Actor(scope) {

    private val authRequest: AuthorizationRequest = UpdateFactoryClientDefaultImpl.context!!.authorizationRequest

    private fun checkNeedAuthorization(state:State):Receive = { msg ->
        when{

            msg is DeploymentInfo && msg.info.deployment.download == DeplBaseResp.Depl.Appl.forced ->{
                state.deploymentManager.send(msg)
            }

            msg is DeploymentInfo && msg.info.deployment.download == DeplBaseResp.Depl.Appl.attempt ->{
                become(waitingDownloadAuthorization(state.copy(deplBaseResp = msg.info)))
                askDownloadAuthorization()
            }

            msg is DeploymentInfo && msg.info.deployment.download == DeplBaseResp.Depl.Appl.skip ->{
                TODO("NOT YET IMPLEMENTED (Appl.skip)")
            }

            else -> unhandled(msg)
        }
    }

    private fun waitingDownloadAuthorization(state: State) :Receive = { msg ->

        when(msg){

            is Message.DownloadGranted -> {
                LOG.info("Authorization granted for download files")
                become(waitingUpdateAuthorization(state))
                state.deploymentManager.send(DeploymentInfo(state.deplBaseResp!!))
            }

            is Message.DownloadDenied  -> { LOG.info("Authorization denied for download files") }

            is Message.AskUpdateAuthorization ->{
                become(waitingUpdateAuthorization(state))
                askUpdateAuthorization()
            }

            is DeploymentInfo ->{
                become(checkNeedAuthorization(state.copy(deplBaseResp = null)))
                channel.send(msg)
            }

            else -> unhandled(msg)

        }

    }

    private fun waitingUpdateAuthorization(state:State) :Receive = { msg ->

        when(msg){

            is Message.UpdateDenied -> { LOG.info("Authorization denied for update") }

            is Message.UpdateGranted -> {
                LOG.info("Authorization granted for update")
                state.deploymentManager.send(START_UPDATING)
            }

            is Message.AskUpdateAuthorization ->{
                askUpdateAuthorization()
            }

            is DeploymentInfo ->{
                become(checkNeedAuthorization(state.copy(deplBaseResp = null)))
                channel.send(msg)
            }

            else -> unhandled(msg)

        }

    }

    private fun askUpdateAuthorization() {
        launch {
            if (authRequest.grantUpdateAuthorization()) {
                channel.send(Message.UpdateGranted)
            } else {
                channel.send(Message.UpdateDenied)
            }
        }
    }

    private fun askDownloadAuthorization() {
        launch {
            if (authRequest.grantDownloadAuthorization()) {
                channel.send(Message.DownloadGranted)
            } else {
                channel.send(Message.DownloadDenied)
            }
        }
    }

    init {
        become(checkNeedAuthorization(State(deploymentManager = DeploymentManager.of(
                scope.coroutineContext,
                channel,
                connectionManager))))
    }

    companion object {
        fun of(context: CoroutineContext,
               parent: ActorRef,
               connectionManager: ActorRef) = Actor.actorOf(context, parent){
            AuthorizationManager(it, parent, connectionManager)
        }

        data class State(val deplBaseResp: DeplBaseResp? = null, val deploymentManager: ActorRef)

        sealed class Message {
            object DownloadGranted: Message()
            object DownloadDenied: Message()
            object UpdateGranted: Message()
            object UpdateDenied: Message()
            object AskUpdateAuthorization: Message()
        }

        private val LOG = LoggerFactory.getLogger(AuthorizationManager::class.java)

    }
}