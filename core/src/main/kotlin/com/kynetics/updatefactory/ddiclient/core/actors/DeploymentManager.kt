/*
 * Copyright © 2017-2021  Kynetics  LLC
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package com.kynetics.updatefactory.ddiclient.core.actors

import com.kynetics.updatefactory.ddiapiclient.api.model.CnclFdbkReq
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplBaseResp
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplBaseResp.Depl.Appl
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplFdbkReq
import com.kynetics.updatefactory.ddiclient.core.actors.ActionManager.Companion.Message.CancelForced
import com.kynetics.updatefactory.ddiclient.core.actors.ActionManager.Companion.Message.UpdateStopped
import com.kynetics.updatefactory.ddiclient.core.actors.ConnectionManager.Companion.Message.Out.DeploymentCancelInfo
import com.kynetics.updatefactory.ddiclient.core.actors.ConnectionManager.Companion.Message.Out.DeploymentInfo
import com.kynetics.updatefactory.ddiclient.core.api.DeploymentPermitProvider
import com.kynetics.updatefactory.ddiclient.core.api.MessageListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.launch

@UseExperimental(ObsoleteCoroutinesApi::class)
class DeploymentManager
private constructor(scope: ActorScope) : AbstractActor(scope) {

    private val registry = coroutineContext[UFClientContext]!!.registry
    private val authRequest: DeploymentPermitProvider = coroutineContext[UFClientContext]!!.deploymentPermitProvider
    private val connectionManager = coroutineContext[CMActor]!!.ref
    private val notificationManager = coroutineContext[NMActor]!!.ref
    private var waitingAuthJob: Job? = null
    private fun beginningReceive(state: State): Receive = { msg ->
        // todo implement download skip option and move content of attempt function to 'msg is DeploymentInfo && msg.downloadIs(attempt)' when case
        suspend fun attempt(msg: DeploymentInfo) {
            val message = "Waiting authorization to download"
            LOG.info(message)
            sendFeedback(message)
            become(waitingDownloadAuthorization(state.copy(deplBaseResp = msg.info)))
            notificationManager.send(MessageListener.Message.State.WaitingDownloadAuthorization)
            waitingAuthJob?.cancel()
            waitingAuthJob = launch {
                val result = authRequest.downloadAllowed().await()
                if (result) {
                    channel.send(Message.DownloadGranted)
                } else {
                    LOG.info("Authorization denied for download files")
                }
                waitingAuthJob = null
            }
        }

        when {

            msg is DeploymentInfo && msg.downloadIs(Appl.forced) -> {
                become(downloadingReceive(state.copy(deplBaseResp = msg.info)))
                child("downloadManager")!!.send(msg)
            }

            msg is DeploymentInfo && msg.downloadIs(Appl.attempt) -> {
                attempt(msg)
            }

            msg is DeploymentInfo && msg.downloadIs(Appl.skip) -> {
                LOG.warn("skip download not yet implemented (used attempt)")
                attempt(msg)
            }

            msg is DeploymentCancelInfo -> {
                stopUpdateAndNotify(msg)
            }

            else -> unhandled(msg)
        }
    }

    private fun waitingDownloadAuthorization(state: State): Receive = { msg ->
        when {

            msg is DeploymentInfo && msg.downloadIs(Appl.attempt) -> {}

            msg is DeploymentInfo -> {
                become(beginningReceive(state))
                channel.send(msg)
            }

            msg is Message.DownloadGranted -> {
                val message = "Authorization granted for downloading files"
                LOG.info(message)
                sendFeedback(message)
                become(downloadingReceive(state))
                child("downloadManager")!!.send(DeploymentInfo(state.deplBaseResp!!))
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

    private fun downloadingReceive(state: State): Receive = { msg ->
        when {

            msg is Message.DownloadFinished && state.updateIs(Appl.forced) -> {
                become(updatingReceive())
                child("updateManager")!!.send(DeploymentInfo(state.deplBaseResp!!))
            }

            msg is Message.DownloadFinished && state.updateIs(Appl.attempt) -> {
                val message = "Waiting authorization to update"
                LOG.info(message)
                sendFeedback(message)
                become(waitingUpdateAuthorization(state))
                notificationManager.send(MessageListener.Message.State.WaitingUpdateAuthorization)
                waitingAuthJob = launch(Dispatchers.IO) {
                    if (authRequest.updateAllowed().await()) {
                        channel.send(Message.UpdateGranted)
                    } else {
                        LOG.info("Authorization denied for update")
                    }
                    waitingAuthJob = null
                }
            }

            msg is Message.DownloadFailed -> {
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

    private fun waitingUpdateAuthorization(state: State): Receive = { msg ->
        when (msg) {

            is DeploymentInfo -> {
                become(downloadingReceive(state.copy(deplBaseResp = msg.info)))
                channel.send(Message.DownloadFinished)
            }

            is Message.UpdateGranted -> {
                val message = "Authorization granted for update"
                LOG.info(message)
                sendFeedback(message)
                become(updatingReceive())
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

    private fun updatingReceive(): Receive = { msg ->
        when (msg) {

            is Message.UpdateFailed -> {
                LOG.info("update failed")
                parent!!.send(msg)
            }

            is Message.UpdateFinished -> {
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

            is CancelForced -> {
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
        channel.cancel()
        notificationManager.send(MessageListener.Message.State.CancellingUpdate)
        parent!!.send(UpdateStopped)
    }

    private fun DeploymentInfo.downloadIs(level: Appl): Boolean {
        return this.info.deployment.download == level
    }

    init {
        actorOf("downloadManager") { DownloadManager.of(it) }
        actorOf("updateManager") { UpdateManager.of(it) }
        become(beginningReceive(State()))
        channel.invokeOnClose {
            waitingAuthJob?.cancel()
        }
    }

    private suspend fun sendFeedback(id: String, vararg messages: String) {
        connectionManager.send(
            ConnectionManager.Companion.Message.In.DeploymentFeedback(
                DeplFdbkReq.newInstance(id,
                    DeplFdbkReq.Sts.Exc.proceeding,
                    DeplFdbkReq.Sts.Rslt.Prgrs(0, 0),
                    DeplFdbkReq.Sts.Rslt.Fnsh.none,
                    *messages
                )
            )
        )
    }

    companion object {
        fun of(scope: ActorScope) = DeploymentManager(scope)

        data class State(val deplBaseResp: DeplBaseResp? = null) {
            fun updateIs(level: Appl): Boolean = deplBaseResp!!.deployment.update == level
        }

        sealed class Message {
            object DownloadGranted : Message()
            object UpdateGranted : Message()
            object DownloadFinished : Message()
            data class DownloadFailed(val details: List<String>) : Message()
            object UpdateFailed : Message()
            object UpdateFinished : Message()
        }
    }
}
