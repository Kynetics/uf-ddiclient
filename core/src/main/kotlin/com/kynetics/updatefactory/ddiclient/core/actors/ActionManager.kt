/*
 * Copyright © 2017-2021  Kynetics  LLC
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package com.kynetics.updatefactory.ddiclient.core.actors

import com.kynetics.updatefactory.ddiapiclient.api.model.CfgDataReq
import com.kynetics.updatefactory.ddiapiclient.api.model.CnclFdbkReq
import com.kynetics.updatefactory.ddiclient.core.actors.ConnectionManager.Companion.Message.In
import com.kynetics.updatefactory.ddiclient.core.actors.ConnectionManager.Companion.Message.Out
import com.kynetics.updatefactory.ddiclient.core.actors.ConnectionManager.Companion.Message.Out.Err.ErrMsg
import com.kynetics.updatefactory.ddiclient.core.api.MessageListener
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.joda.time.Duration

@UseExperimental(ObsoleteCoroutinesApi::class)
class ActionManager
private constructor(scope: ActorScope) : AbstractActor(scope) {

    private val registry = coroutineContext[UFClientContext]!!.registry
    private val configDataProvider = coroutineContext[UFClientContext]!!.configDataProvider
    private val connectionManager = coroutineContext[CMActor]!!.ref
    private val notificationManager = coroutineContext[NMActor]!!.ref

    private fun defaultReceive(state: State): Receive = { msg ->
        when {

            msg is Out.ConfigDataRequired -> {
                val map = configDataProvider.configData()

                if (map.isNotEmpty()) {
                    val cdr = CfgDataReq.of(map, CfgDataReq.Mod.merge)
                    connectionManager.send(In.ConfigDataFeedback(cdr))
                } else {
                    LOG.info("Config data required ignored because of map is empty")
                }
            }

            msg is Out.DeploymentInfo -> onDeployment(msg, state)

            msg is DeploymentManager.Companion.Message.DownloadFailed -> {
                become(defaultReceive(state.copy(deployment = null)))
                child("deploymentManager")!!.close()
            }

            msg is DeploymentManager.Companion.Message.UpdateFailed ||
                    msg is DeploymentManager.Companion.Message.UpdateFinished -> {
                LOG.info(msg.javaClass.simpleName)
                become(defaultReceive(state.copy(deployment = null)))
                child("deploymentManager")!!.close()
                LOG.info("Restore server ping interval")
                connectionManager.send(In.SetPing(null))
            }

            msg is Out.DeploymentCancelInfo -> onCancelInfo(msg, state)

            msg is Message.UpdateStopped -> {
                LOG.info("update stopped")
                become(defaultReceive(state.copy(deployment = null)))
                LOG.info("Restore server ping interval")
                connectionManager.send(In.SetPing(null))
            }

            state.inDeployment && msg is Out.NoAction -> {
                LOG.warn("CancelForced/RemoveTarget.")
                child("deploymentManager")!!.send(Companion.Message.CancelForced)
            }

            msg is Out.NoAction -> onNoAction(msg, state)

            msg is ErrMsg -> {
                LOG.warn("ErrMsg. Not yet implemented")
            }

            else -> unhandled(msg)
        }
    }

    private suspend fun onDeployment(msg: Out.DeploymentInfo, state: State) {
        when {
            state.inDeployment(msg.info.id) -> child("deploymentManager")!!.send(msg)

            state.inDeployment -> child("deploymentManager")!!.send(Companion.Message.CancelForced)

            else -> {
                val deploymentManager = actorOf("deploymentManager") { DeploymentManager.of(it) }
                become(defaultReceive(state.copy(deployment = msg)))
                deploymentManager.send(msg)
                LOG.info("DeploymentInfo msg, decreased ping interval to be reactive on server requests (ping: 30s)")
                connectionManager.send(In.SetPing(Duration.standardSeconds(30)))
            }
        }
    }

    private suspend fun onCancelInfo(msg: Out.DeploymentCancelInfo, state: State) {
        when {
            !state.inDeployment && registry.currentUpdateIsCancellable() -> {
                connectionManager.send(In.CancelFeedback(
                        CnclFdbkReq.newInstance(msg.info.cancelAction.stopId,
                                CnclFdbkReq.Sts.Exc.closed,
                                CnclFdbkReq.Sts.Rslt.Fnsh.success)))
                notificationManager.send(MessageListener.Message.State.CancellingUpdate)
                connectionManager.send(In.SetPing(null))
            }

            !registry.currentUpdateIsCancellable() -> {
                connectionManager.send(In.CancelFeedback(
                        CnclFdbkReq.newInstance(msg.info.cancelAction.stopId,
                                CnclFdbkReq.Sts.Exc.rejected,
                                CnclFdbkReq.Sts.Rslt.Fnsh.success,
                                "Update already started. Can't be stopped.")))
            }

            else -> {
                LOG.warn("DeploymentCancelInfo")
                child("deploymentManager")!!.send(msg)
                LOG.info("Restore server ping interval")
                connectionManager.send(In.SetPing(null))
            }
        }
    }

    private suspend fun onNoAction(msg: Out.NoAction, state: State) {
        when {
            state.inDeployment -> {
                LOG.warn("CancelForced/RemoveTarget.")
                child("deploymentManager")!!.send(Companion.Message.CancelForced)
            }

            else -> {
                notificationManager.send(MessageListener.Message.State.Idle)
            }
        }
    }
    init {
        become(defaultReceive(State()))
        runBlocking { connectionManager.send(In.Register(channel)) }
    }

    companion object {
        fun of(scope: ActorScope) = ActionManager(scope)

        data class State(val deployment: Out.DeploymentInfo? = null) {
            val inDeployment = deployment != null
            fun inDeployment(id: String) = inDeployment && deployment!!.info.id == id
        }

        sealed class Message {

            object CancelForced : Message()
            object UpdateStopped : Message()
        }
    }
}
