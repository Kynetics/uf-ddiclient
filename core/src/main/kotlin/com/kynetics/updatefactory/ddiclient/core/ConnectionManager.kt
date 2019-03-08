package com.kynetics.updatefactory.ddiclient.core

import com.kynetics.updatefactory.ddiapiclient.api.IDdiClient
import com.kynetics.updatefactory.ddiapiclient.api.model.DdiCancel
import com.kynetics.updatefactory.ddiapiclient.api.model.DdiDeploymentBase
import com.kynetics.updatefactory.ddiclient.core.ConnectionManager.Companion.Message.*
import com.kynetics.updatefactory.ddiclient.core.ConnectionManager.Companion.Message.In.*
import com.kynetics.updatefactory.ddiclient.core.ConnectionManager.Companion.Message.Out.*
import com.kynetics.updatefactory.ddiclient.core.ConnectionManager.Companion.Message.Out.Err.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ActorScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import java.time.Duration
import kotlin.coroutines.CoroutineContext

typealias Receiver = Channel<Any>

class ConnectionManager
@UseExperimental(ObsoleteCoroutinesApi::class)
private constructor(scope: ActorScope<In>, private val client: IDdiClient): Actor<In>(scope) {

    private fun stoppedReceive(state: State):Receive<In> =  { msg:In ->
        when(msg) {

            is Start -> become(runningReceive(startPing(state)))

            is SetPing -> become(stoppedReceive(state.copy(clientPingInterval = msg.duration)))

            is Register -> become(stoppedReceive(state.withReceiver(msg.listener)))

            is Unregister -> become(stoppedReceive(state.withoutReceiver(msg.listener)))

            is Stop -> {}

            else -> unhandled(msg)

        }
    }

    private fun runningReceive(state: State):Receive<In> = { msg: In ->
        when(msg) {

            is Stop -> become(stoppedReceive(stopPing(state)))


            is SetPing -> if(msg.duration != state.clientPingInterval) {
                    become(runningReceive(startPing(state.copy(clientPingInterval = msg.duration))))
                }

            is Ping -> {

                try {
                    val res = client.getControllerActions()
                    //println(res)
                    if(res.requireConfigData()){
                        this.send(ConfigDataRequired, state)
                    }
                    if(res.requireDeployment()) {
                        val res2 = client.getDeploymentActionDetails(res.actionId())
                        this.send(DeploymentInfo(res2), state)
                    }
                    if(res.requireCancel()) {
                        val res2 = client.getCancelActionDetails(res.actionId())
                        this.send(DeploymentCancelInfo(res2), state)
                    }

                    val newState = state.withServerSleep(res.config.polling.sleep).withoutBackoff()
                    if(newState != state) become(runningReceive(startPing(newState)))
                } catch (t: Throwable) {
                    this.send(ErrMsg(
                            "exception: ${t.javaClass}"+ if(t.message != null) " message: ${t.message}" else ""
                    ), state)
                    become(runningReceive(startPing(state.nextBackoff())))
                }
            }

            is Register -> become(runningReceive(state.withReceiver(msg.listener)))

            is Unregister -> become(runningReceive(state.withoutReceiver(msg.listener)))

            is Start -> {}

        }
    }

    private fun startPing(state:State):State = stopPing(state).copy(timer =
        launch {
            while(true) {
                if(state.hasBackoff) {
                    delay(state.pingInterval)
                    channel.send(Ping)
                } else {
                    channel.send(Ping)
                    delay(state.pingInterval)
                }
            }
        }
    )

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
        fun of(context: CoroutineContext, client: IDdiClient): SendChannel<In> = Actor.actorOf(context) {
            ConnectionManager(it,client)
        }

        private data class State(
                val serverPingInterval:Duration = Duration.ofSeconds(1),
                val clientPingInterval:Duration? = null,
                val backoffPingInterval:Duration? = null,
                val timer: Job? = null,
                val receivers: Set<Receiver> = emptySet()
        ) {
            val pingInterval = when {
                        backoffPingInterval != null -> backoffPingInterval
                        clientPingInterval != null -> clientPingInterval
                        else -> serverPingInterval
                    }
            fun nextBackoff() = if(backoffPingInterval == null)
                this.copy(backoffPingInterval = Duration.ofSeconds(1))
                else this.copy(backoffPingInterval = minOf(backoffPingInterval.multipliedBy(2), Duration.ofMinutes(1)))

            fun withoutBackoff() = if(backoffPingInterval != null) this.copy(backoffPingInterval = null) else this

            fun withServerSleep(sleep:String):State {
                fun sleepStr2duration(str:String): Duration {
                    val fields = str.split(':').map { Integer.parseInt(it).toLong()}.toTypedArray()
                    return Duration.ofHours(fields[0]).plusMinutes(fields[1]).plusSeconds(fields[2])
                }
                val newServerPingInterval = sleepStr2duration(sleep)
                return if(newServerPingInterval != serverPingInterval) this.copy(serverPingInterval=newServerPingInterval)
                else this
            }

            val hasBackoff = this.backoffPingInterval != null



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
            }

            open class Out: Message(){
                object ConfigDataRequired: Out()
                data class DeploymentInfo(val info: DdiDeploymentBase): Out()
                data class DeploymentCancelInfo(val info: DdiCancel): Out()

                sealed class Err: Out() {
                    data class ErrMsg(val message:String): Err()
                }

            }

        }
    }

}

