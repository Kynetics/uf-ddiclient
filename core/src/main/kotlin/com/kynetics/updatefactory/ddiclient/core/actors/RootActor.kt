package com.kynetics.updatefactory.ddiclient.core.actors

import com.kynetics.updatefactory.ddiclient.core.*
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ActorScope
import kotlin.coroutines.CoroutineContext

@UseExperimental(ObsoleteCoroutinesApi::class)
class RootActor
@UseExperimental(ObsoleteCoroutinesApi::class)
private constructor(scope: ActorScope<Any>): Actor(scope) {

    private fun mainReceive(state: State): Receive = { msg ->
        when(msg) {
            is Message.Start ->
                state.connectionManager.send(ConnectionManager.Companion.Message.In.Start)

            is Message.Stop ->
                state.connectionManager.send(ConnectionManager.Companion.Message.In.Stop)

            is Message.ForcePing ->
                state.connectionManager.send(ConnectionManager.Companion.Message.In.Ping)

            else -> unhandled(msg)
        }
    }

    init {
        val cm = ConnectionManager.of(scope.coroutineContext, this.channel)
        val am = ActionManager.of(scope.coroutineContext, this.channel, cm)
        become(mainReceive(State(am, cm)))
    }


    companion object {
        fun of(context: CoroutineContext) = Actor.actorOf(context, null) {
            RootActor(it)
        }

        data class State(
                val actionManager:ActorRef,
                val connectionManager: ActorRef)

        sealed class Message{
            object Start:Message()
            object Stop:Message()
            object ForcePing: Message()
        }
    }
}