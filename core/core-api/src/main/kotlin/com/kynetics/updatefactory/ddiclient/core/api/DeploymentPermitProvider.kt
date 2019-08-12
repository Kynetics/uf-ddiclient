package com.kynetics.updatefactory.ddiclient.core.api

interface DeploymentPermitProvider {

    fun downloadAllowed(): Boolean = true

    fun updateAllowed(): Boolean = true
}
