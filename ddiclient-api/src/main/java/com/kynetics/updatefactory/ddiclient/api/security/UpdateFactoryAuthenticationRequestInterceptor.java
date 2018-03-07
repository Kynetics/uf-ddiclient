/*
 * Copyright © 2017-2018 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.updatefactory.ddiclient.api.security;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.kynetics.updatefactory.ddiclient.api.api.DdiRestConstants.*;
import static com.kynetics.updatefactory.ddiclient.api.security.Authentication.AuthenticationType.TARGET_TOKEN_AUTHENTICATION;
import static com.kynetics.updatefactory.ddiclient.api.security.Authentication.newInstance;

/**
 * @author Daniele Sergio
 */
public class UpdateFactoryAuthenticationRequestInterceptor implements Interceptor {

    public interface OnTargetTokenFound{
        void onFound(String targetToken);
    }

    public UpdateFactoryAuthenticationRequestInterceptor(Set<Authentication> authentications, OnTargetTokenFound onTargetTokenFound) {
        Objects.requireNonNull(authentications);
        this.authentications = authentications;
        this.onTargetTokenFound = onTargetTokenFound;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        final Request originalRequest = chain.request();

        final Request.Builder builder = originalRequest.newBuilder();

        final boolean isConfigDataRequest = originalRequest.url().toString().endsWith(CONFIG_DATA_ACTION);

        Authentication targetTokenAuth = null;

        for(Authentication authentication:authentications){
            builder.addHeader(authentication.getHeader(), authentication.getHeaderValue());
            if(authentication.getType() == TARGET_TOKEN_AUTHENTICATION){
                targetTokenAuth = authentication;
            }
        }

        if(isConfigDataRequest){
            builder.header(TARGET_TOKEN_REQUEST_HEADER_NAME, String.valueOf(true));
        }

        final Response response = chain.proceed(builder.build());

        final String targetToken = response.header(TARGET_TOKEN_HEADER_NAME);

        if (isConfigDataRequest && targetToken != null){
            authentications.add(newInstance(TARGET_TOKEN_AUTHENTICATION, targetToken));
            onTargetTokenFound.onFound(targetToken);
            authentications.remove(targetTokenAuth);
        }

        return response;
    }


    private final Set<Authentication> authentications;
    private final OnTargetTokenFound onTargetTokenFound;
}
