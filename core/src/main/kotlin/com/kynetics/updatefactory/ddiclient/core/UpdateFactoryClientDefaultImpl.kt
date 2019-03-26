package com.kynetics.updatefactory.ddiclient.core

import com.kynetics.updatefactory.ddiapiclient.api.DdiClientDefaultImpl
import com.kynetics.updatefactory.ddiclient.core.actors.AbstractActor
import com.kynetics.updatefactory.ddiclient.core.actors.ActorRef
import com.kynetics.updatefactory.ddiclient.core.actors.ConnectionManager.Companion.Message.In.*
import com.kynetics.updatefactory.ddiclient.core.actors.RootActor
import com.kynetics.updatefactory.ddiclient.core.actors.UFClientContext
import com.kynetics.updatefactory.ddiclient.core.api.*
import kotlinx.coroutines.runBlocking

class UpdateFactoryClientDefaultImpl: UpdateFactoryClient {

    var rootActor:ActorRef?=null

    override fun init(
            updateFactoryClientData: UpdateFactoryClientData,
            directoryForArtifactsProvider: DirectoryForArtifactsProvider,
            configDataProvider: ConfigDataProvider,
            deploymentPermitProvider: DeploymentPermitProvider,
            eventListeners: List<EventListener>,
            vararg updaters: Updater) {
        rootActor = AbstractActor.actorOf("rootActor", UFClientContext(
                DdiClientDefaultImpl.of(updateFactoryClientData),
                UpdaterRegistry(*updaters),
                configDataProvider,
                directoryForArtifactsProvider,
                deploymentPermitProvider,
                eventListeners
        )){ RootActor.of(it)}
    }

    override fun startAsync() = runBlocking { rootActor!!.send(Start) }

    override fun stop() = runBlocking { rootActor!!.send(Stop) }

    override fun forcePing() = runBlocking { rootActor!!.send(Ping) }
}