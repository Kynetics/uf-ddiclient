package com.kynetics.updatefactory.ddiclient.core.actors

import com.kynetics.updatefactory.ddiapiclient.api.DdiClient
import com.kynetics.updatefactory.ddiclient.core.UpdaterRegistry
import com.kynetics.updatefactory.ddiclient.core.api.DeploymentPermitProvider
import com.kynetics.updatefactory.ddiclient.core.api.ConfigDataProvider
import com.kynetics.updatefactory.ddiclient.core.api.DirectoryForArtifactsProvider
import com.kynetics.updatefactory.ddiclient.core.api.EventListener
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import org.slf4j.Logger
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

    protected fun child(name:String) = childs[name]

    protected fun become(receive: Receive) { __receive__ = receive }

    protected val LOG = LoggerFactory.getLogger(this::class.java)

    protected fun unhandled(msg: Any) {
        if(LOG.isWarnEnabled){
            LOG.warn("received unexpected message $msg in ${coroutineContext[CoroutineName]} actor")
        }
    }

    protected val parent: ActorRef? = coroutineContext[ParentActor]?.ref

    protected val name: String = coroutineContext[CoroutineName]!!.name

    override val channel: Channel<Any> = object : Channel<Any> by actorScope.channel {
        override suspend fun send(element: Any) {
            if(LOG.isDebugEnabled){
                LOG.debug("Send message ${element.javaClass.simpleName} to actor $name.")
            }
            actorScope.channel.send(element)
        }

    }

    protected fun <T:AbstractActor> actorOf(
            name:String,
            context: CoroutineContext = EmptyCoroutineContext,
            capacity: Int = 3,
            start: CoroutineStart = CoroutineStart.DEFAULT,
            onCompletion: CompletionHandler? = null,
            block: suspend (ActorScope) -> T): ActorRef {
        val childRef = actorScope.actor<Any>(
                Dispatchers.IO.plus(CoroutineName(name)).plus(ParentActor(this.channel)).plus(context),
                capacity,start,onCompletion){ __workflow__(LOG,block)() }
        childs.put(name,childRef)
        return childRef
    }

    companion object {

        private fun <T:AbstractActor> __workflow__(logger:Logger, block: suspend (ActorScope) -> T): suspend ActorScope.() -> Unit = {
            try {
                val actor = block(this)
                if(actor.LOG.isInfoEnabled){
                    actor.LOG.info("Actor ${actor.name} created.")
                }
                try {
                    for (message in channel) { actor.__receive__(message) }
                    if(actor.LOG.isInfoEnabled){
                        actor.LOG.info("Actor ${actor.name} exiting.")
                    }
                } catch (t:Throwable){
                    actor.LOG.error("Error processing message in actor ${actor.name}. error: ${t.javaClass} message: ${t.message}")
                    if(actor.parent != null) {
                        actor.parent.send(ActorException(actor.name, actor.channel, t))
                    } else {
                        throw t
                    }
                }
            } catch (t:Throwable) {
                val name = coroutineContext[CoroutineName]?.name
                val parent = coroutineContext[ParentActor]?.ref
                logger.error("Error creating actor ${name?:"Unknown"}. error: ${t.javaClass} message: ${t.message}")
                if(parent != null) {
                    parent.send(ActorCreationException(name?:"Unknown", channel, t))
                } else {
                    throw t
                }
            }
        }

        fun <T:AbstractActor> actorOf(
                name:String,
                context: CoroutineContext = EmptyCoroutineContext,
                capacity: Int = 3,
                start: CoroutineStart = CoroutineStart.DEFAULT,
                onCompletion: CompletionHandler? = null,
                block: suspend (ActorScope) -> T): ActorRef =
                GlobalScope.actor(Dispatchers.IO.plus(CoroutineName(name)).plus(context), capacity, start, onCompletion){
                    __workflow__(LoggerFactory.getLogger(AbstractActor::class.java),block)()
                }
    }
}


data class UFClientContext(
        val ddiClient: DdiClient,
        val registry: UpdaterRegistry,
        val configDataProvider: ConfigDataProvider,
        val directoryForArtifactsProvider: DirectoryForArtifactsProvider,
        val deploymentPermitProvider: DeploymentPermitProvider,
        val eventListeners: List<EventListener>
): AbstractCoroutineContextElement(UFClientContext){
    companion object Key : CoroutineContext.Key<UFClientContext>
    override fun toString(): String = "UFClientContext($this)"
}

data class ParentActor(val ref:ActorRef): AbstractCoroutineContextElement(ParentActor){
    companion object Key : CoroutineContext.Key<ParentActor>
    override fun toString(): String = "ParentActor($ref)"
}

class ActorException(val actorName: String, val actorRef: ActorRef, throwable: Throwable): Exception(throwable)
class ActorCreationException(val actorName: String, val actorRef: ActorRef, throwable: Throwable): Exception(throwable)