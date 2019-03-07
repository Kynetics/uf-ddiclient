package com.kynetics.updatefactory.ddiclient.core

import com.kynetics.updatefactory.ddiclient.core.ConnectionManager.Companion.Message
import kotlinx.coroutines.Job
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ActorScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import java.time.Duration
import kotlin.coroutines.CoroutineContext

class ConnectionManager @ObsoleteCoroutinesApi
private constructor(scope: ActorScope<Message>, private val client: DdiClient): Actor<Message>(scope) {

    private fun stoppedReceive(state: State):Receive<Message> =  { msg:Message ->
        when(msg) {

            is Message.Start -> become(runningReceive(startPing(state)))

            is Message.SetPing -> become(stoppedReceive(state.copy(clientPingInterval = msg.duration)))

            is Message.Stop -> {}

            else -> unhandled(msg)

        }
    }

    private fun runningReceive(state: State):Receive<Message> = { msg: Message ->
        when(msg) {

            is Message.Stop -> become(stoppedReceive(stopPing(state)))


            is Message.SetPing -> if(msg.duration != state.clientPingInterval) {
                    become(runningReceive(startPing(state.copy(clientPingInterval = msg.duration))))
                }

            is Message.Ping -> {
                try {
                    val res = client.getControllerActions()
                    println(res)
                    val newState = state.withServerSleep(res.config.polling.sleep).withoutBackoff()
                    if(newState != state) become(runningReceive(startPing(newState)))
                } catch (t: Throwable) {
                    println(t)
                    become(runningReceive(startPing(state.nextBackoff())))
                }
            }

            is Message.Start -> {}

        }
    }

    private fun startPing(state:State):State = stopPing(state).copy(timer =
        launch {
            while(true) {
                if(state.hasBackoff) {
                    delay(state.pingInterval)
                    channel.send(Message.Ping)
                } else {
                    channel.send(Message.Ping)
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

    private fun unhandled(msg: Message) {
        println("received unexpected message $msg")
    }

    init {
        become(stoppedReceive(State()))
    }

    companion object {
        @ObsoleteCoroutinesApi
        fun of(context: CoroutineContext, client: DdiClient): SendChannel<Message> = Actor.actorOf(context) {
            ConnectionManager(it,client)
        }

        data class State(
                val serverPingInterval:Duration = Duration.ofSeconds(1),
                val clientPingInterval:Duration? = null,
                val backoffPingInterval:Duration? = null,
                val timer: Job? = null) {
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
        }

        sealed class Message {
            object Start : Message()
            object Stop : Message()
            object Ping : Message()
            data class SetPing(val duration: Duration?) : Message()
        }
    }

}

