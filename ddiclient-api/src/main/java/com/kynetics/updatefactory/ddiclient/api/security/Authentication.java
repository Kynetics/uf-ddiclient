/*
 * Copyright © 2017 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.updatefactory.ddiclient.api.security;

/**
 * @author Daniele Sergio
 */
public class Authentication {
    public enum AuthenticationType {
        TARGET_TOKEN_AUTHENTICATION("TargetToken"),
        GATEWAY_TOKEN_AUTHENTICATION("GatewayToken"),
        ANONYMOUS_AUTHENTICATION("");

        private final String header;

        AuthenticationType(String header) {
            this.header = header;
        }

        public String getHeader() {
            return header;
        }
    }

    public Authentication(AuthenticationType type, String token) {
        this.type = type;
        this.token = token;
    }

    public AuthenticationType getType() {
        return type;
    }

    public String getToken() {
        return token;
    }

    private final AuthenticationType type;

    private final String token;
}
