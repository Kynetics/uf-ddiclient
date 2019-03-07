package com.kynetics.updatefactory.ddiclient.core

import com.kynetics.updatefactory.ddiclient.core.ConnectionManager.Companion.Message.In
import com.kynetics.updatefactory.ddiclient.core.ConnectionManager.Companion.Message.Out.*
import com.kynetics.updatefactory.ddiclient.core.ConnectionManager.Companion.Message.Out.Err.ErrMsg
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ActorScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class ActionManager @UseExperimental(ObsoleteCoroutinesApi::class)
private constructor(scope: ActorScope<Any>, cm: SendChannel<In>): Actor<Any>(scope) {

    private fun defaultReceive():Receive<Any> = { msg ->
        when(msg) {

            is ConfigDataRequired -> {
                println(msg)
            }

            is DeploymentInfo -> {
                println(msg)
            }

            is DeploymentCancelInfo -> {
                println(msg)
            }

            is ErrMsg -> {
                println(msg)
            }

            else -> unhandled(msg)

        }
    }

    init {
        become(defaultReceive())
        launch { cm.send(In.Register(channel)) }
    }

    companion object {
        fun of(context: CoroutineContext, cm: SendChannel<In>): SendChannel<Any> = Actor.actorOf(context) {
            ActionManager(it, cm)
        }
    }
}