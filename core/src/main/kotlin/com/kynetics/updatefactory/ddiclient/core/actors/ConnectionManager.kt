package com.kynetics.updatefactory.ddiclient.core.actors

import com.kynetics.updatefactory.ddiapiclient.api.DdiClient
import com.kynetics.updatefactory.ddiapiclient.api.model.*
import com.kynetics.updatefactory.ddiclient.core.actors.ConnectionManager.Companion.Message.In.*
import com.kynetics.updatefactory.ddiclient.core.actors.ConnectionManager.Companion.Message.Out
import com.kynetics.updatefactory.ddiclient.core.actors.ConnectionManager.Companion.Message.Out.*
import com.kynetics.updatefactory.ddiclient.core.actors.ConnectionManager.Companion.Message.Out.Err.ErrMsg
import com.kynetics.updatefactory.ddiclient.core.api.EventListener
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.launch
import org.joda.time.Duration
import org.joda.time.Instant
import java.util.*
import kotlin.concurrent.timer

//TODO use ticker for ping
@UseExperimental(ObsoleteCoroutinesApi::class)
class ConnectionManager
private constructor(scope: ActorScope): AbstractActor(scope) {

    private val client:DdiClient = coroutineContext[UFClientContext]!!.ddiClient
    private val notificationManager = coroutineContext[NMActor]!!.ref

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
                LOG.info("Execute ping calls to the server...")
                val s = state.copy(lastPing = Instant.now())
                try {

                    client.onControllerActionsChange(state.controllerBaseEtag){ res, newControllerBaseEtag ->
                        if(res.requireConfigData()){
                            this.send(ConfigDataRequired, state)
                        }

                        var actionFound = false
                        var etag = state.deploymentEtag
                        if(res.requireDeployment()) {
                            client.onDeploymentActionDetailsChange(res.deploymentActionId(),0, state.deploymentEtag) { deplBaseResp, newDeploymentEtag ->
                                etag = newDeploymentEtag
                                this.send(DeploymentInfo(deplBaseResp), state)
                            }
                            actionFound = true
                        }

                        if(res.requireCancel()) {
                            val res2 = client.getCancelActionDetails(res.cancelActionId())
                            this.send(DeploymentCancelInfo(res2), state)
                            actionFound = true
                        }

                        if(!actionFound){
                            this.send(NoAction, state)
                        }

                        val newState = s.copy(controllerBaseEtag = newControllerBaseEtag, deploymentEtag = etag)
                                .withServerSleep(res.config.polling.sleep)
                                .withoutBackoff()
                        become(runningReceive(startPing(newState)))

                    }


                } catch (t: Throwable) {
                    fun loopMsg(t:Throwable):String = t.message + if(t.cause!=null) " ${loopMsg(t.cause!!)}" else ""
                    val errorDetails = "exception: ${t.javaClass} message: ${loopMsg(t)}"
                    this.send(ErrMsg(errorDetails), state)
                    LOG.warn(t.message, t)
                    become(runningReceive(startPing(s.nextBackoff())))
                    notificationManager.send(EventListener.Event.Error(listOf(errorDetails)))
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

            is CancelFeedback -> {
                try {
                    client.postCancelActionFeedback(msg.feedback.id, msg.feedback)
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

    private fun startPing(state: State): State {
        val now = Instant.now()
        val elapsed = Duration(state.lastPing, now)
        val timer = timer(name ="Polling",
                initialDelay = Math.max(state.pingInterval.minus(elapsed).millis, 0),
                period = Math.max(state.pingInterval.millis, 5_000)){
            launch{
                channel.send(Ping)
            }
        }
        return stopPing(state).copy(timer = timer)
    }

    private fun stopPing(state: State): State = if(state.timer!=null) {
        state.timer.cancel()
        state.copy(timer = null)
    } else {
        state
    }

    private suspend fun send(msg: Out, state: State) {
        state.receivers.forEach { it.send(msg) }
    }

    init {
        become(stoppedReceive(State()))
    }

    companion object {
        fun of(scope: ActorScope) = ConnectionManager(scope)

        private data class State(
                val serverPingInterval: Duration = Duration.standardSeconds(0),
                val clientPingInterval: Duration? = null,
                val backoffPingInterval: Duration? = null,
                val lastPing: Instant? = Instant.EPOCH,
                val deploymentEtag: String = "",
                val controllerBaseEtag: String = "",
                val timer: Timer? = null,
                val receivers: Set<ActorRef> = emptySet()
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

            fun withServerSleep(sleep:String): State {
                fun sleepStr2duration(str:String): Duration {
                    val fields = str.split(':').map { Integer.parseInt(it).toLong()}.toTypedArray()
                    return Duration.standardHours(fields[0]).plus(
                            Duration.standardMinutes(fields[1])) .plus(
                            Duration.standardSeconds(fields[2]))
                }
                val newServerPingInterval = sleepStr2duration(sleep)
                return if(newServerPingInterval != serverPingInterval) this.copy(serverPingInterval=newServerPingInterval)
                else this
            }
            fun withReceiver(receiver: ActorRef) = this.copy(receivers = receivers+receiver)

            fun withoutReceiver(receiver: ActorRef) = this.copy(receivers = receivers-receiver)
        }

        sealed class Message {

            sealed class In: Message(){
                object Start : In()
                object Stop : In()
                object Ping : In()
                data class Register(val listener: ActorRef): In()
                data class Unregister(val listener: ActorRef): In()
                data class SetPing(val duration: Duration?) : In()
                data class DeploymentFeedback(val feedback: DeplFdbkReq)
                data class CancelFeedback(val feedback: CnclFdbkReq)
                data class ConfigDataFeedback(val cfgDataReq: CfgDataReq)
            }

            open class Out: Message(){
                object ConfigDataRequired: Out()
                data class DeploymentInfo(val info: DeplBaseResp): Out(){
                    fun equalsApartHistory(other: DeploymentInfo): Boolean {
                        fun pruneMessages(dbr: DeplBaseResp) = dbr.copy(actionHistory = dbr.actionHistory?.copy(messages = emptyList()))
                        return pruneMessages(info) == pruneMessages(other.info)
                    }
                }
                data class DeploymentCancelInfo(val info: CnclActResp): Out()

                object NoAction: Out()

                sealed class Err: Out() {
                    data class ErrMsg(val message:String): Err()
                }
            }
        }
    }
}

