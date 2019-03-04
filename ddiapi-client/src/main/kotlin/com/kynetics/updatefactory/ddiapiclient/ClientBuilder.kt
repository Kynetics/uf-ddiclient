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

import com.kynetics.updatefactory.ddiapiclient.api.DdiRestApi
import com.kynetics.updatefactory.ddiapiclient.security.Authentication
import com.kynetics.updatefactory.ddiapiclient.security.HawkbitAuthenticationRequestInterceptor
import com.kynetics.updatefactory.ddiapiclient.security.UpdateFactoryAuthenticationRequestInterceptor
import com.kynetics.updatefactory.ddiapiclient.security.UpdateFactoryAuthenticationRequestInterceptor.OnTargetTokenFound
import okhttp3.OkHttpClient
import retrofit2.converter.gson.GsonConverterFactory

import java.util.HashSet
import java.util.concurrent.Executors

import com.kynetics.updatefactory.ddiapiclient.ServerType.HAWKBIT
import com.kynetics.updatefactory.ddiapiclient.security.Authentication.AuthenticationType.GATEWAY_TOKEN_AUTHENTICATION
import com.kynetics.updatefactory.ddiapiclient.security.Authentication.AuthenticationType.TARGET_TOKEN_AUTHENTICATION
import com.kynetics.updatefactory.ddiapiclient.security.Authentication.Companion.newInstance
import com.kynetics.updatefactory.ddiapiclient.validation.Assert.Companion.NotNull
import com.kynetics.updatefactory.ddiapiclient.validation.Assert.Companion.ValidateUrl
import retrofit2.Retrofit.Builder

/**
 * @author Daniele Sergio
 */
class ClientBuilder {
    private val builder: Builder
    private var serverType = HAWKBIT
    private var baseUrl: String? = null
    private val authentications = HashSet<Authentication>()
    private var okHttpBuilder: OkHttpClient.Builder? = null
    private var onTargetTokenFound: OnTargetTokenFound? = null

    init {
        this.builder = Builder()
    }

    fun withBaseUrl(baseUrl: String): ClientBuilder {
        this.baseUrl = baseUrl
        return this
    }

    fun withHttpBuilder(okHttpBuilder: OkHttpClient.Builder): ClientBuilder {
        this.okHttpBuilder = okHttpBuilder
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

    fun withGatewayToken(token: String?): ClientBuilder {
        if (token == null || token.isEmpty()) {
            return this
        }
        authentications.add(newInstance(GATEWAY_TOKEN_AUTHENTICATION, token))
        return this
    }

    fun withTargetToken(token: String?): ClientBuilder {
        if (token == null || token.isEmpty()) {
            return this
        }
        authentications.add(newInstance(TARGET_TOKEN_AUTHENTICATION, token))
        return this
    }

    fun build(): DdiRestApi {
        ValidateUrl(baseUrl, "baseUrl")
        NotNull(okHttpBuilder, "okHttpBuilder")
        NotNull(serverType, "serverType")
        okHttpBuilder!!.interceptors().add(0, if (serverType === HAWKBIT)
            HawkbitAuthenticationRequestInterceptor(authentications)
        else
            UpdateFactoryAuthenticationRequestInterceptor(authentications, onTargetTokenFound))

        return builder
                .baseUrl(baseUrl!!)
                .addConverterFactory(GsonConverterFactory.create())
                .callbackExecutor(Executors.newSingleThreadExecutor())
                .client(okHttpBuilder!!.build())
                .build()
                .create(DdiRestApi::class.java)
    }

}
