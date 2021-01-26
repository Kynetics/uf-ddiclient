/*
 * Copyright © 2017-2021  Kynetics  LLC
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package com.kynetics.updatefactory.ddiapiclient.security

import java.io.IOException
import java.util.ArrayList
import java.util.Objects
import okhttp3.Interceptor
import okhttp3.Response

/**
 * @author Daniele Sergio
 */
class HawkbitAuthenticationRequestInterceptor(authentications: Set<Authentication>) : Interceptor {

    private val authentications: List<Authentication>
    private var authenticationUse = 0

    init {
        Objects.requireNonNull<Set<Authentication>>(authentications)
        this.authentications = ArrayList(authentications)
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        if (authentications.isEmpty()) {
            return chain.proceed(chain.request())
        }

        val originalRequest = chain.request()

        val builder = originalRequest.newBuilder()
        val size = authentications.size
        val exitValue = authenticationUse
        var response: Response
        do {
            val authentication = authentications[authenticationUse]
            builder.header(authentication.header, authentication.headerValue)
            response = chain.proceed(builder.build())
            if (response.code() != 401) {
                break
            }
            authenticationUse = ++authenticationUse % size
        } while (authenticationUse != exitValue)

        return response
    }
}
