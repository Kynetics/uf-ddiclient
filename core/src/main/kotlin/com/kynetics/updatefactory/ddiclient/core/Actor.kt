package com.kynetics.updatefactory.ddiclient.core

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ActorScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlin.coroutines.CoroutineContext

typealias Receive = suspend (Any) -> Unit

typealias ActorRef = SendChannel<Any>

@UseExperimental(ObsoleteCoroutinesApi::class)
abstract class Actor constructor(scope: ActorScope<Any>): ActorScope<Any> by scope {

    private var _receive: Receive = { ; }

    fun become(receive: Receive) {
        _receive = receive
    }

    companion object {
        fun actorOf(
                context: CoroutineContext,
                parent: ActorRef?,
                init: (ActorScope<Any>) -> Actor
        ): ActorRef = GlobalScope.actor(context, capacity = 10) {
            val instance = init(this@actor)
            println("Actor ${this@actor.javaClass.simpleName} created.")
            for (msg in channel) instance._receive(msg)
            println("Actor ${this@actor.javaClass.simpleName} terminated.")
        }
    }

    protected fun unhandled(msg: Any) {
        println("received unexpected message $msg")
    }

}