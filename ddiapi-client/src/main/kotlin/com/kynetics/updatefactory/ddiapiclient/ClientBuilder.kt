/*
 * Copyright © 2017-2019 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.updatefactory.ddiapiclient

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.kynetics.updatefactory.ddiapiclient.api.DdiRestApi
import com.kynetics.updatefactory.ddiapiclient.security.Authentication
import com.kynetics.updatefactory.ddiapiclient.security.HawkbitAuthenticationRequestInterceptor
import com.kynetics.updatefactory.ddiapiclient.security.UpdateFactoryAuthenticationRequestInterceptor
import com.kynetics.updatefactory.ddiapiclient.api.OnTargetTokenFound
import okhttp3.OkHttpClient
import retrofit2.converter.gson.GsonConverterFactory

import java.util.HashSet
import java.util.concurrent.Executors

import com.kynetics.updatefactory.ddiapiclient.ServerType.HAWKBIT
import com.kynetics.updatefactory.ddiapiclient.api.DdiClient
import com.kynetics.updatefactory.ddiapiclient.api.IDdiClient
import com.kynetics.updatefactory.ddiapiclient.security.Authentication.AuthenticationType.GATEWAY_TOKEN_AUTHENTICATION
import com.kynetics.updatefactory.ddiapiclient.security.Authentication.AuthenticationType.TARGET_TOKEN_AUTHENTICATION
import com.kynetics.updatefactory.ddiapiclient.security.Authentication.Companion.newInstance
import com.kynetics.updatefactory.ddiapiclient.validation.Assert.Companion.notEmpty
import com.kynetics.updatefactory.ddiapiclient.validation.Assert.Companion.notNull
import com.kynetics.updatefactory.ddiapiclient.validation.Assert.Companion.validateUrl
import retrofit2.Retrofit.Builder

/**
 * @author Daniele Sergio
 */
//TODO better check invaraints! i.e onTargetTokenFound != null ==> servertype == UPDATE SERVER
class ClientBuilder {

    private val builder = Builder()
    private val httpBuilder = OkHttpClient.Builder()
    private val authentications = HashSet<Authentication>()

    private var serverType = HAWKBIT
    private var baseUrl: String? = null
    private var onTargetTokenFound: OnTargetTokenFound? = null
    private var tenant: String = "TEST"
    private var controllerId: String? = null

    fun withBaseUrl(baseUrl: String): ClientBuilder {
        notEmpty(baseUrl, "baseUrl")
        this.baseUrl = baseUrl
        return this
    }

    fun withTetnat(tenant: String): ClientBuilder {
        notEmpty(tenant, "tenant")
        this.tenant = tenant
        return this
    }

    fun withControllerId(controllerId: String): ClientBuilder {
        notEmpty(controllerId, "controllerId")
        this.controllerId = controllerId
        return this
    }

    fun withServerType(serverType: ServerType): ClientBuilder {
        this.serverType = serverType
        return this
    }

    fun withOnTargetTokenFound(onTargetTokenFound: OnTargetTokenFound): ClientBuilder {
        this.onTargetTokenFound = onTargetTokenFound
        return this
    }

    fun withGatewayToken(token: String): ClientBuilder {
        notEmpty(token, "gatewayToken")
        authentications.add(newInstance(GATEWAY_TOKEN_AUTHENTICATION, token))
        return this
    }

    fun withTargetToken(token: String): ClientBuilder {
        notEmpty(token, "targetToken")
        authentications.add(newInstance(TARGET_TOKEN_AUTHENTICATION, token))
        return this
    }

    fun build(): IDdiClient {
        validateUrl(baseUrl, "baseUrl")
        notNull(serverType, "serverType")
        notNull(controllerId, "controllerId")
        httpBuilder.interceptors().add(0, if (serverType === HAWKBIT)
            HawkbitAuthenticationRequestInterceptor(authentications)
        else
            UpdateFactoryAuthenticationRequestInterceptor(authentications, onTargetTokenFound))
        val ddiRestApi = builder
                .baseUrl(baseUrl!!)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(CoroutineCallAdapterFactory())
                .callbackExecutor(Executors.newSingleThreadExecutor())
                .client(httpBuilder.build())
                .build()
                .create(DdiRestApi::class.java)
        return DdiClient(ddiRestApi, tenant, controllerId!!)
    }

}
