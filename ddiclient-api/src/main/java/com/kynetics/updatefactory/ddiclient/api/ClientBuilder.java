/*
 * Copyright © 2017 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.updatefactory.ddiclient.api;

import com.kynetics.updatefactory.ddiclient.api.api.DdiRestApi;
import com.kynetics.updatefactory.ddiclient.api.security.Authentication;
import com.kynetics.updatefactory.ddiclient.api.security.HawkbitAuthenticationRequestInterceptor;
import com.kynetics.updatefactory.ddiclient.api.security.UpdateFactoryAuthenticationRequestInterceptor;
import com.kynetics.updatefactory.ddiclient.api.security.UpdateFactoryAuthenticationRequestInterceptor.OnTargetTokenFound;
import okhttp3.OkHttpClient;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.kynetics.updatefactory.ddiclient.api.ServerType.HAWKBIT;
import static com.kynetics.updatefactory.ddiclient.api.security.Authentication.AuthenticationType.GATEWAY_TOKEN_AUTHENTICATION;
import static com.kynetics.updatefactory.ddiclient.api.security.Authentication.AuthenticationType.TARGET_TOKEN_AUTHENTICATION;
import static com.kynetics.updatefactory.ddiclient.api.security.Authentication.newInstance;
import static retrofit2.Retrofit.Builder;

/**
 * @author Daniele Sergio
 */
public class ClientBuilder {

    private ServerType serverType = HAWKBIT;
    private String baseUrl;
    private Set<Authentication> authentications = new HashSet<>();
    private final Builder builder;
    private OkHttpClient.Builder okHttpBuilder;
    private OnTargetTokenFound onTargetTokenFound;

    public ClientBuilder() {
        this.builder = new Builder();
    }

    public ClientBuilder withBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public ClientBuilder withHttpBuilder(OkHttpClient.Builder okHttpBuilder){
        this.okHttpBuilder = okHttpBuilder;
        return this;
    }

    public ClientBuilder withServerType(ServerType serverType) {
        this.serverType = serverType;
        return this;
    }

    public ClientBuilder withOnTargetTokenFound(OnTargetTokenFound onTargetTokenFound) {
        this.onTargetTokenFound = onTargetTokenFound;
        return this;
    }

    public ClientBuilder withGatewayToken(String token) {
        if(token == null || token.isEmpty()){
            return this;
        }
        authentications.add(newInstance(GATEWAY_TOKEN_AUTHENTICATION, token));
        return this;
    }

    public ClientBuilder withTargetToken(String token) {
        if (token == null || token.isEmpty()) {
            return this;
        }
        authentications.add(newInstance(TARGET_TOKEN_AUTHENTICATION, token));
        return this;
    }

    public DdiRestApi build(){
        okHttpBuilder.interceptors().add(0,serverType == HAWKBIT ?
                new HawkbitAuthenticationRequestInterceptor(authentications) :
                new UpdateFactoryAuthenticationRequestInterceptor(authentications, onTargetTokenFound));

        return builder
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpBuilder.build())
                .build()
                .create(DdiRestApi.class);
    }

}
