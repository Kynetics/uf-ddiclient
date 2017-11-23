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

        private static final String HEADER = "Authorization";

        private final String type;

        AuthenticationType(String type) {
            this.type = type;
        }

        private String getType() {
            return type;
        }

        private String getHeader(){
            return HEADER;
        }
    }

    public Authentication(AuthenticationType type, String token) {
        this.type = type;
        this.token = token;
    }

    AuthenticationType getType() {
        return type;
    }

    String getToken() {
        return token;
    }

    String getHeaderValue() {
        return String.format(HEADER_VALUE_TEMPLATE, type.getType(), token);
    }

    String getHeader(){
        return type.getHeader();
    }

    private final AuthenticationType type;

    private final String token;

    private static final String HEADER_VALUE_TEMPLATE = "%s %s";
}
