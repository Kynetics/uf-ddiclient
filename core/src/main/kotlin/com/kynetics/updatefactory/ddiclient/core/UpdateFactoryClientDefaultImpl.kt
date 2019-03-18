package com.kynetics.updatefactory.ddiclient.core

import com.kynetics.updatefactory.ddiapiclient.api.DdiClient
import com.kynetics.updatefactory.ddiapiclient.api.DdiClientDefaultImpl
import com.kynetics.updatefactory.ddiclient.core.actors.AbstractActor
import com.kynetics.updatefactory.ddiclient.core.actors.RootActor
import com.kynetics.updatefactory.ddiclient.core.actors.UFClientContext
import com.kynetics.updatefactory.ddiclient.core.api.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking

class UpdateFactoryClientDefaultImpl: UpdateFactoryClient {

    override fun init(
            updateFactoryClientData: UpdateFactoryClientData,
            directoryForArtifactsProvider: DirectoryForArtifactsProvider,
            configDataProvider: ConfigDataProvider,
            authorizationRequest: AuthorizationRequest,
            vararg updaters: Updater) {
        context = Context(
                DdiClientDefaultImpl.of(updateFactoryClientData),
                UpdaterRegistry(*updaters),
                configDataProvider,
                directoryForArtifactsProvider,
                authorizationRequest)
        rootActor = AbstractActor.actorOf("rootActor", UFClientContext(
               context!!.ddiClient,
                context!!.registry,
                context!!.configDataProvider,
                context!!.directoryForArtifactsProvider
        )){ RootActor.of(it)}
    }

    override fun startAsync() {
        runBlocking {
            rootActor!!.send(RootActor.Companion.Message.Start)
        }
    }

    override fun stop() {
        runBlocking {
            rootActor!!.send(RootActor.Companion.Message.Stop)
        }
    }

    override fun forcePing() {
        runBlocking {
            rootActor!!.send(RootActor.Companion.Message.ForcePing)
        }
    }

    data class Context(
            val ddiClient: DdiClient,
            val registry: UpdaterRegistry,
            val configDataProvider: ConfigDataProvider,
            val directoryForArtifactsProvider: DirectoryForArtifactsProvider,
            val authorizationRequest: AuthorizationRequest)

    companion object {
        var context: Context? = null
        var rootActor: ActorRef? = null
    }
}