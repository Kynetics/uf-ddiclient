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
import com.kynetics.updatefactory.ddiclient.api.security.AuthenticationRequestInterceptor;
import com.kynetics.updatefactory.ddiclient.api.security.Authentication;
import okhttp3.OkHttpClient;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static retrofit2.Retrofit.Builder;

/**
 * @author Daniele Sergio
 */
public class ClientBuilder {

    private String baseUrl;
    private List<Authentication> authentications;
    private long connectionTimeout = 10;
    private final Builder builder;

    public ClientBuilder() {
        this.builder = new Builder();
    }

    public ClientBuilder withBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public ClientBuilder withAutheintications(List<Authentication> authentications) {
        this.authentications = authentications;
        return this;
    }

    public ClientBuilder withConnectionTimeout(long second){
        connectionTimeout = second;
        return this;
    }

    public DdiRestApi build(){
        return builder
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(buildOkHttpClient())
                .build()
                .create(DdiRestApi.class);
    }

    private OkHttpClient buildOkHttpClient(){
        return new OkHttpClient.Builder()
                .addInterceptor(new AuthenticationRequestInterceptor(authentications))
                .connectTimeout(connectionTimeout, TimeUnit.SECONDS)
                .build();
    }
}
