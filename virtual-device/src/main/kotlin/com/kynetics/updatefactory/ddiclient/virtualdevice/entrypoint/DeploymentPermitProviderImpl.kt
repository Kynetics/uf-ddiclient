/*
 * Copyright © 2017-2021  Kynetics  LLC
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

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