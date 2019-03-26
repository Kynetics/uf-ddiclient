package com.kynetics.updatefactory.ddiclient.core.actors

import com.kynetics.updatefactory.ddiapiclient.api.model.CnclFdbkReq
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplBaseResp
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplBaseResp.Depl.Appl
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplBaseResp.Depl.Appl.*
import com.kynetics.updatefactory.ddiclient.core.actors.ConnectionManager.Companion.Message.Out.*
import com.kynetics.updatefactory.ddiclient.core.actors.DeploymentManager.Companion.Message.*
import com.kynetics.updatefactory.ddiclient.core.actors.ActionManager.Companion.Message.*
import com.kynetics.updatefactory.ddiclient.core.api.DeploymentPermitProvider
import com.kynetics.updatefactory.ddiclient.core.api.EventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.withContext


@UseExperimental(ObsoleteCoroutinesApi::class)
class DeploymentManager
private constructor(scope: ActorScope): AbstractActor(scope) {

    private val authRequest: DeploymentPermitProvider = coroutineContext[UFClientContext]!!.deploymentPermitProvider
    private val connectionManager = coroutineContext[CMActor]!!.ref
    private val notificationManager = coroutineContext[NMActor]!!.ref
    private fun beginningReceive(state: State):Receive = { msg ->
        when{

            msg is DeploymentInfo && msg.downloadIs(forced)  -> {
                become(downloadingReceive(state.copy(deplBaseResp = msg.info)))
                child("downloadManager")!!.send(msg)
            }

            msg is DeploymentInfo && msg.downloadIs(attempt) -> {
                become(waitingDownloadAuthorization(state.copy(deplBaseResp = msg.info)))
                channel.send(withContext(Dispatchers.IO) {
                    if (authRequest.downloadAllowed()) DownloadGranted else DownloadDenied
                })
            }

            msg is DeploymentInfo && msg.downloadIs(skip) -> {
                TODO("NOT YET IMPLEMENTED (Appl.skip)")
            }

            else -> unhandled(msg)
        }
    }

    private fun waitingDownloadAuthorization(state: State) :Receive = { msg ->
        when(msg){

            msg is DeploymentInfo && msg.downloadIs(attempt) -> {}

            is DeploymentInfo -> {
                become(beginningReceive(state))
                channel.send(msg)
            }

            is DownloadGranted -> {
                LOG.info("Authorization granted for downloading files")
                become(downloadingReceive(state))
                child("downloadManager")!!.send(DeploymentInfo(state.deplBaseResp!!))
            }

            is DownloadDenied -> {
                LOG.info("Authorization denied for download files")
                become(beginningReceive(state))
            }

            is DeploymentCancelInfo -> {
                stopUpdateAndNotify(msg)
            }

            msg is CancelForced -> {
                stopUpdate()
            }

            else -> unhandled(msg)

        }

    }

    private fun downloadingReceive(state: State): Receive = { msg ->
        when{

            msg is DownloadFinished && state.updateIs(forced) -> {
                become(updatingReceive(state))
                child("updateManager")!!.send(DeploymentInfo(state.deplBaseResp!!))
            }

            msg is DownloadFinished &&  state.updateIs(attempt) -> {
                become(waitingUpdateAuthorization(state))
                channel.send(withContext(Dispatchers.IO) {
                    if (authRequest.updateAllowed()) UpdateGranted else UpdateDenied
                })
            }

            msg is DownloadFailed -> {
                //TODO handle download failed
                LOG.error("download failed")
                parent!!.send(msg)
            }

            msg is DeploymentCancelInfo -> {
                stopUpdateAndNotify(msg)
            }

            msg is CancelForced -> {
                stopUpdate()
            }

            else -> unhandled(msg)
        }
    }

    private fun waitingUpdateAuthorization(state: State) :Receive = { msg ->
        when(msg){

            is DeploymentInfo -> {// TODO check this case
                become(beginningReceive(state.copy(deplBaseResp = null)))
                channel.send(msg)
            }

            is Message.UpdateDenied -> {
                LOG.info("Authorization denied for update")
                become(beginningReceive(state))
            }

            is Message.UpdateGranted -> {
                LOG.info("Authorization granted for update")
                become(updatingReceive(state))
                child("updateManager")!!.send(DeploymentInfo(state.deplBaseResp!!))
            }

            is DeploymentCancelInfo -> {
                stopUpdateAndNotify(msg)
            }

            is CancelForced -> {
                stopUpdate()
            }

            else -> unhandled(msg)

        }

    }

    private fun updatingReceive(state: State): Receive = { msg ->
        when(msg) {

            is UpdateFailed -> {
                LOG.error("update failed")
                parent!!.send(msg)
            }

            is UpdateFinished -> {
                LOG.info("update finished")
                parent!!.send(msg)
            }

            is DeploymentCancelInfo -> {
                LOG.info("can't stop update")
                connectionManager.send(ConnectionManager.Companion.Message.In.CancelFeedback(
                        CnclFdbkReq.newInstance(msg.info.cancelAction.stopId,
                                CnclFdbkReq.Sts.Exc.rejected,
                                CnclFdbkReq.Sts.Rslt.Fnsh.success,
                                "Update already started. Can't be stopped.")))
            }

            is CancelForced ->{
                LOG.info("Force cancel ignored")
            }
        }
    }


    private suspend fun stopUpdateAndNotify(msg: DeploymentCancelInfo) {
        connectionManager.send(ConnectionManager.Companion.Message.In.CancelFeedback(
                CnclFdbkReq.newInstance(msg.info.cancelAction.stopId,
                        CnclFdbkReq.Sts.Exc.closed,
                        CnclFdbkReq.Sts.Rslt.Fnsh.success)))
        stopUpdate()
    }

    private suspend fun stopUpdate() {
        LOG.info("Stopping update")
        child("downloadManager")?.close()
        child("updateManager")?.close()
        channel.close()
        notificationManager.send(EventListener.Event.UpdateCancelled)
        parent!!.send(UpdateStopped)
    }


    private fun DeploymentInfo.downloadIs(level:Appl):Boolean {
        return this.info.deployment.download == level
    }

    init {
        actorOf("downloadManager"){ DownloadManager.of(it)}
        actorOf("updateManager"){UpdateManager.of(it)}
        become(beginningReceive(State()))
    }

    companion object {
        fun of(scope: ActorScope) = DeploymentManager(scope)

        data class State(val deplBaseResp: DeplBaseResp? = null){
            fun updateIs(level:Appl):Boolean = deplBaseResp!!.deployment.update == level
        }

        sealed class Message {
            object DownloadGranted: Message()
            object DownloadDenied: Message()
            object UpdateGranted: Message()
            object UpdateDenied: Message()
            object DownloadFinished: Message()
            object DownloadFailed: Message()
            object UpdateFailed: Message()
            object UpdateFinished: Message()
        }

    }
}