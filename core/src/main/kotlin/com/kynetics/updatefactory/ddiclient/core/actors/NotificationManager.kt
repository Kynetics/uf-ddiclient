package com.kynetics.updatefactory.ddiclient.core.actors

import com.kynetics.updatefactory.ddiclient.core.api.EventListener
import kotlinx.coroutines.ObsoleteCoroutinesApi

@UseExperimental(ObsoleteCoroutinesApi::class)
class NotificationManager
private constructor(scope: ActorScope): AbstractActor(scope) {

    private val listeners = coroutineContext[UFClientContext]!!.eventListeners

    private fun defaultReceive():Receive = { msg ->

        when (msg) {

            is EventListener.Event -> listeners.forEach { it.onEvent(msg) }

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