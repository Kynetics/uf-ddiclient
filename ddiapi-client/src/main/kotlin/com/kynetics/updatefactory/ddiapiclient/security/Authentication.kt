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

import com.kynetics.updatefactory.ddiapiclient.security.Authentication.AuthenticationType.ANONYMOUS_AUTHENTICATION

/**
 * @author Daniele Sergio
 */
class Authentication private constructor(internal val type: AuthenticationType, token: String) {

    internal val headerValue: String
    internal val header: String

    init {
        headerValue = String.format(HEADER_VALUE_TEMPLATE, type.type, token)
        header = type.header
    }

    enum class AuthenticationType constructor(internal val type: String) {
        TARGET_TOKEN_AUTHENTICATION("TargetToken"),
        GATEWAY_TOKEN_AUTHENTICATION("GatewayToken"),
        ANONYMOUS_AUTHENTICATION("");

        internal val header = "Authorization"
    }

    companion object {

        fun newInstance(type: AuthenticationType, token: String): Authentication {
            return if (type == ANONYMOUS_AUTHENTICATION) ANONYMOUS else Authentication(type, token)
        }

        private var ANONYMOUS: Authentication = Authentication(ANONYMOUS_AUTHENTICATION, "")

        private const val HEADER_VALUE_TEMPLATE = "%s %s"
    }
}
