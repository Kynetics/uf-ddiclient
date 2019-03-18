package com.kynetics.updatefactory.ddiclient.core.actors

import com.kynetics.updatefactory.ddiclient.core.ActionManager
import com.kynetics.updatefactory.ddiclient.core.ConnectionManager
import kotlinx.coroutines.ObsoleteCoroutinesApi

@UseExperimental(ObsoleteCoroutinesApi::class)
class RootActor
@UseExperimental(ObsoleteCoroutinesApi::class)
private constructor(scope: ActorScope): AbstractActor(scope) {

    private fun mainReceive(state: State): Receive = { msg ->
        when(msg) {
            is Message.Start ->
                state.connectionManager.send(ConnectionManager.Message.In.Start)

            is Message.Stop ->
                state.connectionManager.send(ConnectionManager.Message.In.Stop)

            is Message.ForcePing ->
                state.connectionManager.send(ConnectionManager.Message.In.Ping)

            else -> unhandled(msg)
        }
    }

    init {
        val cm = actorOf("connectionManager"){ConnectionManager.of(this)}
        val am = ActionManager.of(scope.coroutineContext, this.channel, cm)
        become(mainReceive(State(am, cm)))
    }


    companion object {
        fun of(scope: ActorScope) = RootActor(scope)

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