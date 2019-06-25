package com.kynetics.updatefactory.ddiclient.core.actors

import com.kynetics.updatefactory.ddiclient.core.api.MessageListener
import kotlinx.coroutines.ObsoleteCoroutinesApi

@UseExperimental(ObsoleteCoroutinesApi::class)
class NotificationManager
private constructor(scope: ActorScope): AbstractActor(scope) {

    private val listeners = coroutineContext[UFClientContext]!!.messageListeners

    private fun defaultReceive():Receive = { msg ->

        when (msg) {

            is MessageListener.Message -> listeners.forEach { it.onMessage(msg) }

            else -> unhandled(msg)

        }

    }

    init {
        become(defaultReceive())
    }

    companion object {
        fun of(scope: ActorScope) = NotificationManager(scope)

    }

}