package com.kynetics.updatefactory.ddiclient.core.actors

import com.kynetics.updatefactory.ddiapiclient.api.DdiClient
import com.kynetics.updatefactory.ddiclient.core.Actor
import com.kynetics.updatefactory.ddiclient.core.UpdaterRegistry
import com.kynetics.updatefactory.ddiclient.core.api.ConfigDataProvider
import com.kynetics.updatefactory.ddiclient.core.api.DirectoryForArtifactsProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import org.slf4j.LoggerFactory
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

typealias Receive = suspend (Any) -> Unit

typealias ActorRef = SendChannel<Any>

@UseExperimental(ObsoleteCoroutinesApi::class)
typealias ActorScope = kotlinx.coroutines.channels.ActorScope<Any>

val EmptyReceive: Receive = {}


@UseExperimental(ObsoleteCoroutinesApi::class)
abstract class AbstractActor protected constructor(private val actorScope: ActorScope): ActorScope by actorScope {

    private var __receive__:Receive = EmptyReceive

    private val childs:MutableMap<String, ActorRef> = emptyMap<String, ActorRef>().toMutableMap()

    protected fun become(receive: Receive) { __receive__ = receive }

    private val LOG = LoggerFactory.getLogger(this::class.java)

    protected fun unhandled(msg: Any) {
        if(LOG.isWarnEnabled){
            LOG.warn("received unexpected message $msg in ${coroutineContext[CoroutineName]} actor")
        }
    }

    protected fun <T:AbstractActor> actorOf(
            name:String,
            context: CoroutineContext = EmptyCoroutineContext,
            capacity: Int = 0,
            start: CoroutineStart = CoroutineStart.DEFAULT,
            onCompletion: CompletionHandler? = null,
            block: suspend (ActorScope) -> T): ActorRef {
        val childRef = actorScope.actor<Any>(
                context.plus(CoroutineName(name)).plus(ParentActor(this.channel)),
                capacity,start,onCompletion){ __workflow__(block)() }
        childs.put(name,childRef)
        return childRef
    }

    companion object {

        private fun <T:AbstractActor> __workflow__(block: suspend (ActorScope) -> T): suspend ActorScope.() -> Unit = {
            try {
                val actor = block(this)
                try {
                    for (message in channel) { actor.__receive__(message) }
                } catch (t:Throwable){
                    // todo get parent from context and sent error to it
                    t.printStackTrace()
                }
            } catch (t:Throwable) {
                // todo get parent from context and sent error to it
                t.printStackTrace()
            }
        }

        fun <T:AbstractActor> actorOf(
                name:String,
                context: CoroutineContext = EmptyCoroutineContext,
                capacity: Int = 0,
                start: CoroutineStart = CoroutineStart.DEFAULT,
                onCompletion: CompletionHandler? = null,
                block: suspend (ActorScope) -> T): ActorRef =
                GlobalScope.actor(context.plus(CoroutineName(name)),capacity,start,onCompletion){
                    __workflow__(block)()
                }
    }
}


data class UFClientContext(
        val ddiClient: DdiClient,
        val registry: UpdaterRegistry,
        val configDataProvider: ConfigDataProvider,
        val directoryForArtifactsProvider: DirectoryForArtifactsProvider
): AbstractCoroutineContextElement(UFClientContext){
    companion object Key : CoroutineContext.Key<UFClientContext>
    override fun toString(): String = "UFClientContext($this)"
}

data class ParentActor(val ref:ActorRef): AbstractCoroutineContextElement(ParentActor){
    companion object Key : CoroutineContext.Key<ParentActor>
    override fun toString(): String = "ParentActor($ref)"
}