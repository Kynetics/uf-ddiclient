package com.kynetics.updatefactory.ddiclient.core.api

interface UpdateFactoryClient {

    fun init(
            updateFactoryClientData: UpdateFactoryClientData,
            directoryForArtifactsProvider: DirectoryForArtifactsProvider,
            configDataProvider: ConfigDataProvider,
            authorizationRequest: AuthorizationRequest,
            vararg updaters: Updater)

    fun startAsync()

    fun stop()

    fun forcePing()
}