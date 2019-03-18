package com.kynetics.updatefactory.ddiclient.core.api

interface AuthorizationRequest {

    fun grantDownloadAuthorization():Boolean

    fun grantUpdateAuthorization(): Boolean

}