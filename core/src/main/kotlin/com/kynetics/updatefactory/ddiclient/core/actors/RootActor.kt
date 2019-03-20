package com.kynetics.updatefactory.ddiclient.core.actors

import com.kynetics.updatefactory.ddiclient.core.actors.ConnectionManager.Companion.Message.In.*
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

@UseExperimental(ObsoleteCoroutinesApi::class)
class RootActor
private constructor(scope: ActorScope): AbstractActor(scope) {

    private fun mainReceive(): Receive = { msg ->
        when(msg) {
            is Start, Ping -> child("connectionManager")!!.send(msg)

            is Stop -> {
                child("actionManager")!!.close()
                child("connectionManager")!!.send(msg)
                child("connectionManager")!!.close()
                channel.close()
            }

            else -> unhandled(msg)
        }
    }

    init {
        val cmActor = actorOf("connectionManager"){ ConnectionManager.of(it)}
        val ctxt = CMActor(cmActor)
        actorOf("actionManager", ctxt){ ActionManager.of(it)}
        become(mainReceive())
    }

    companion object {
        fun of(scope: ActorScope) = RootActor(scope)
    }
}

data class CMActor(val ref:ActorRef): AbstractCoroutineContextElement(CMActor){
    companion object Key : CoroutineContext.Key<CMActor>
    override fun toString(): String = "CMActor($ref)"
}
