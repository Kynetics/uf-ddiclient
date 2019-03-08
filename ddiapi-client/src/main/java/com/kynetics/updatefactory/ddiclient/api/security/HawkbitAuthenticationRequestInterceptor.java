/*
 * Copyright © 2017-2019 Kynetics LLC
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author Daniele Sergio
 */
public class HawkbitAuthenticationRequestInterceptor implements Interceptor {

    public HawkbitAuthenticationRequestInterceptor(Set<Authentication> authentications) {
        Objects.requireNonNull(authentications);
        this.authentications = new ArrayList<>(authentications);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        if(authentications.size() == 0){
            return chain.proceed(chain.request());
        }

        final Request originalRequest = chain.request();

        final Request.Builder builder = originalRequest.newBuilder();
        final int size = authentications.size();
        final int exitValue = authenticationUse;
        Response response = null;
        do{
            final Authentication authentication = authentications.get(authenticationUse);
            builder.header(authentication.getHeader(), authentication.getHeaderValue());
            response = chain.proceed(builder.build());
            if(response.code() != 401){
                break;
            }
            authenticationUse = ++authenticationUse % size;
        }while(authenticationUse != exitValue);

        return response;
    }



    private final List<Authentication> authentications;
    private int authenticationUse = 0;
}
