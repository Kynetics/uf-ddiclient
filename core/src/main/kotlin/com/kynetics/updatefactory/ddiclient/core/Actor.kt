package com.kynetics.updatefactory.ddiclient.core

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ActorScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext

typealias Receive = suspend (Any) -> Unit

typealias ActorRef = SendChannel<Any>

@UseExperimental(ObsoleteCoroutinesApi::class)
abstract class Actor constructor(private val scope: ActorScope<Any>): ActorScope<Any> by scope {

    private var _receive: Receive = { ; }

    fun become(receive: Receive) {
        _receive = receive
    }

    override val channel: Channel<Any> = LoggerChannel(scope.channel, "ActorName")

    companion object {
        fun actorOf(
                context: CoroutineContext,
                parent: ActorRef?,
                init: (ActorScope<Any>) -> Actor
        ): ActorRef = GlobalScope.actor(context, capacity = 10) {
            val instance = init(this@actor)
            for (msg in channel){
                instance._receive(msg)
            }
            LOG.info("Actor terminated.") //todo replace Actor with its name
        }

        private val LOG = LoggerFactory.getLogger(Actor::class.java)

    }

    protected fun unhandled(msg: Any) {
        if(LOG.isWarnEnabled){
            LOG.warn("received unexpected message $msg") //todo add name of actor
        }
    }

    private class LoggerChannel(val ch:Channel<Any>, val actorName: String) :Channel<Any> by ch{
        override suspend fun send(element: Any) {
            if(LOG.isDebugEnabled){
                LOG.debug("$element send to $actorName")
            }
            ch.send(element)
        }

    }
}

