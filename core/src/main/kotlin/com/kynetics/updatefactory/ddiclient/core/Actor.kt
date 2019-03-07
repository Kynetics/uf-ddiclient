package com.kynetics.updatefactory.ddiclient.core

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ActorScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlin.coroutines.CoroutineContext

typealias Receive<T> = suspend (T) -> Unit

abstract class Actor<T>(scope: ActorScope<T>): ActorScope<T> by scope {

    private var _receive: Receive<T> = { t -> ; }

    fun become(receive: Receive<T>) {
        _receive = receive
    }

    companion object {
        fun <T> actorOf(
                context: CoroutineContext,
                init: (ActorScope<T>) -> Actor<T>
        ): SendChannel<T> = GlobalScope.actor(context) {
            val instance = init(this@actor)
            for (msg in channel) instance._receive(msg)
        }
    }
}