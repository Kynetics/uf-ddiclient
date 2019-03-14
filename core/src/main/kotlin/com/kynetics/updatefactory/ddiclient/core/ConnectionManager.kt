package com.kynetics.updatefactory.ddiclient.core

import com.kynetics.updatefactory.ddiapiclient.api.DdiClient
import com.kynetics.updatefactory.ddiapiclient.api.model.CfgDataReq
import com.kynetics.updatefactory.ddiapiclient.api.model.CnclActResp
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplBaseResp
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplFdbkReq
import com.kynetics.updatefactory.ddiclient.core.ConnectionManager.Companion.Message.In.*
import com.kynetics.updatefactory.ddiclient.core.ConnectionManager.Companion.Message.Out
import com.kynetics.updatefactory.ddiclient.core.ConnectionManager.Companion.Message.Out.*
import com.kynetics.updatefactory.ddiclient.core.ConnectionManager.Companion.Message.Out.Err.ErrMsg
import kotlinx.coroutines.Job
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ActorScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.joda.time.Duration
import org.joda.time.Instant
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext

typealias Receiver = Channel<Any>

class ConnectionManager
@UseExperimental(ObsoleteCoroutinesApi::class)
private constructor(scope: ActorScope<Any>): Actor(scope) {

    private val client:DdiClient = UpdateFactoryClientDefaultImpl.context!!.ddiClient

    private fun stoppedReceive(state: State):Receive =  { msg ->
        when(msg) {

            is Start -> become(runningReceive(startPing(state)))

            is Stop -> {}

            is Register -> become(stoppedReceive(state.withReceiver(msg.listener)))

            is Unregister -> become(stoppedReceive(state.withoutReceiver(msg.listener)))

            is SetPing -> become(stoppedReceive(state.copy(clientPingInterval = msg.duration, lastPing = Instant.EPOCH)))

            else -> unhandled(msg)
        }
    }

    private fun runningReceive(state: State):Receive = { msg ->
        when(msg) {

            is Start -> {}

            is Stop -> become(stoppedReceive(stopPing(state)))

            is Register -> become(runningReceive(state.withReceiver(msg.listener)))

            is Unregister -> become(runningReceive(state.withoutReceiver(msg.listener)))

            is SetPing -> become(runningReceive(startPing(state.copy(clientPingInterval = msg.duration,  lastPing = Instant.EPOCH))))

            is Ping -> {
                LOG.info("in ping")
                val s = state.copy(lastPing = Instant.now())
                try {
                    val res = client.getControllerActions()
                    //println(res)
                    if(res.requireConfigData()){
                        this.send(ConfigDataRequired, state)
                    }
                    if(res.requireDeployment()) {
                        val res2 = client.getDeploymentActionDetails(res.deploymentActionId())
                        this.send(DeploymentInfo(res2), state)
                    }
                    if(res.requireCancel()) {
                        val res2 = client.getCancelActionDetails(res.cancelActionId())
                        this.send(DeploymentCancelInfo(res2), state)
                    }

                    val newState = s.withServerSleep(res.config.polling.sleep).withoutBackoff()
                    become(runningReceive(startPing(newState)))
                } catch (t: Throwable) {
                    fun loopMsg(t:Throwable):String = t.message + if(t.cause!=null) " ${loopMsg(t.cause!!)}" else ""
                     this.send(ErrMsg(
                            "exception: ${t.javaClass} message: ${loopMsg(t)}"
                    ), state)
                    LOG.warn(t.message, t)
                    become(runningReceive(startPing(s.nextBackoff())))
                }
            }

            is DeploymentFeedback -> {
                try {
                    client.postDeploymentActionFeedback(msg.feedback.id, msg.feedback)
                } catch (t: Throwable) {
                    this.send(ErrMsg("exception: ${t.javaClass}"+ if(t.message != null) " message: ${t.message}" else ""), state)
                    LOG.warn(t.message, t)
                }
            }

            is ConfigDataFeedback -> {
                try {
                    client.putConfigData(msg.cfgDataReq)
                } catch (t: Throwable) {
                    this.send(ErrMsg("exception: ${t.javaClass}"+ if(t.message != null) " message: ${t.message}" else ""), state)
                    LOG.warn(t.message, t)
                }
            }

            else -> {
                unhandled(msg)
            }
        }
    }

    private fun startPing(state:State):State {
        val now = Instant.now()
        val elapsed = Duration(state.lastPing, now)
        val sleepTime = state.pingInterval.minus(elapsed)
        val timer = launch {
            delay(sleepTime)
            channel.send(Ping)
        }
        return stopPing(state).copy(timer = timer)
    }

    private fun stopPing(state:State):State = if(state.timer!=null) {
            state.timer.cancel()
            state.copy(timer = null)
        } else { state
    }

    private suspend fun send(msg: Out, state: State) {
        state.receivers.forEach { it.send(msg) }
    }

    init {
        become(stoppedReceive(State()))
    }

    companion object {
        fun of(context: CoroutineContext, parent: ActorRef) = Actor.actorOf(context, parent) {
            ConnectionManager(it)
        }

        private data class State(
                val serverPingInterval:Duration = Duration.standardSeconds(0),
                val clientPingInterval:Duration? = null,
                val backoffPingInterval:Duration? = null,
                val lastPing:Instant? = Instant.EPOCH,
                val timer: Job? = null,
                val receivers: Set<Receiver> = emptySet()
        ) {
            val pingInterval = when {
                        backoffPingInterval != null -> backoffPingInterval
                        clientPingInterval != null -> clientPingInterval
                        else -> serverPingInterval
                    }
            fun nextBackoff() = if(backoffPingInterval == null)
                this.copy(backoffPingInterval = Duration.standardSeconds(1))
                else this.copy(backoffPingInterval = minOf(backoffPingInterval.multipliedBy(2), Duration.standardMinutes(1)))

            fun withoutBackoff() = if(backoffPingInterval != null) this.copy(backoffPingInterval = null) else this

            fun withServerSleep(sleep:String):State {
                fun sleepStr2duration(str:String): Duration {
                    val fields = str.split(':').map { Integer.parseInt(it).toLong()}.toTypedArray()
                    return Duration.standardHours  (fields[0])  .plus(
                           Duration.standardMinutes(fields[1])) .plus(
                           Duration.standardSeconds(fields[2]))
                }
                val newServerPingInterval = sleepStr2duration(sleep)
                return if(newServerPingInterval != serverPingInterval) this.copy(serverPingInterval=newServerPingInterval)
                else this
            }
            fun withReceiver(receiver: Receiver) = this.copy(receivers = receivers+receiver)

            fun withoutReceiver(receiver: Receiver) = this.copy(receivers = receivers-receiver)
        }

        sealed class Message {

            sealed class In: Message(){
                object Start : In()
                object Stop : In()
                object Ping : In()
                data class Register(val listener: Receiver): In()
                data class Unregister(val listener: Receiver): In()
                data class SetPing(val duration: Duration?) : In()
                data class DeploymentFeedback(val feedback: DeplFdbkReq)
                data class ConfigDataFeedback(val cfgDataReq: CfgDataReq)
            }

            open class Out: Message(){
                object ConfigDataRequired: Out()
                data class DeploymentInfo(val info: DeplBaseResp): Out()
                data class DeploymentCancelInfo(val info: CnclActResp): Out()

                sealed class Err: Out() {
                    data class ErrMsg(val message:String): Err()
                }

            }

        }

        private val LOG = LoggerFactory.getLogger(ConnectionManager::class.java)

    }

}

