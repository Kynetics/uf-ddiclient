package com.kynetics.updatefactory.ddiclient.core.api

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

interface DeploymentPermitProvider {

    fun downloadAllowed(): Deferred<Boolean> = CompletableDeferred(true)

    fun updateAllowed(): Deferred<Boolean> = CompletableDeferred(true)
}
