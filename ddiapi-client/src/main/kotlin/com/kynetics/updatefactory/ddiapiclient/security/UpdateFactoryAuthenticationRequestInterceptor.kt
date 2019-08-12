/*
 * Copyright © 2017-2019 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.updatefactory.ddiapiclient.security

import com.kynetics.updatefactory.ddiapiclient.api.DdiRestConstants.Companion.CONFIG_DATA_ACTION
import com.kynetics.updatefactory.ddiapiclient.api.DdiRestConstants.Companion.TARGET_TOKEN_HEADER_NAME
import com.kynetics.updatefactory.ddiapiclient.api.DdiRestConstants.Companion.TARGET_TOKEN_REQUEST_HEADER_NAME
import com.kynetics.updatefactory.ddiapiclient.security.Authentication.AuthenticationType.TARGET_TOKEN_AUTHENTICATION
import com.kynetics.updatefactory.ddiapiclient.security.Authentication.Companion.newInstance
import com.kynetics.updatefactory.ddiclient.core.api.TargetTokenFoundListener
import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Response

/**
 * @author Daniele Sergio
 */
class UpdateFactoryAuthenticationRequestInterceptor(
    private val authentications: MutableSet<Authentication>,
    private val targetTokenFoundListener: TargetTokenFoundListener =
            object : TargetTokenFoundListener {}
) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val builder = originalRequest.newBuilder()

        val isConfigDataRequest = originalRequest.url().toString().endsWith(CONFIG_DATA_ACTION)

        var targetTokenAuth: Authentication? = null

        for (authentication in authentications) {
            builder.addHeader(authentication.header, authentication.headerValue)
            if (authentication.type === TARGET_TOKEN_AUTHENTICATION) {
                targetTokenAuth = authentication
            }
        }

        if (isConfigDataRequest) {
            builder.header(TARGET_TOKEN_REQUEST_HEADER_NAME, true.toString())
        }

        val response = chain.proceed(builder.build())

        val targetToken = response.header(TARGET_TOKEN_HEADER_NAME)

        if (isConfigDataRequest && targetToken != null) {
            authentications.add(newInstance(TARGET_TOKEN_AUTHENTICATION, targetToken))
            targetTokenFoundListener.onFound(targetToken)
            authentications.remove(targetTokenAuth)
        }

        return response
    }
}
