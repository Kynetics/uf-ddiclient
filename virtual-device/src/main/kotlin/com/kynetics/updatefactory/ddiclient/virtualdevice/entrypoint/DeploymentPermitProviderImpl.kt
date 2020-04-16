package com.kynetics.updatefactory.ddiclient.virtualdevice.entrypoint

import com.kynetics.updatefactory.ddiclient.core.api.DeploymentPermitProvider
import com.kynetics.updatefactory.ddiclient.virtualdevice.Configuration
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

class DeploymentPermitProviderImpl: DeploymentPermitProvider {
    override fun downloadAllowed(): Deferred<Boolean> {
        return CompletableDeferred(Configuration.grantDownload)
    }

    override fun updateAllowed(): Deferred<Boolean> {
        return CompletableDeferred(Configuration.grantUpdate)
    }
}