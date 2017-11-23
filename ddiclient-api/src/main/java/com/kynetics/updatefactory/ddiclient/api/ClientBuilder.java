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
import com.kynetics.updatefactory.ddiclient.api.security.AuthenticationRequestInterceptor;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.List;

import static retrofit2.Retrofit.Builder;

/**
 * @author Daniele Sergio
 */
public class ClientBuilder {

    private String baseUrl;
    private List<Authentication> authentications;
    private final Builder builder;
    private OkHttpClient.Builder okHttpBuilder;

    public ClientBuilder() {
        this.builder = new Builder();
    }

    public ClientBuilder withBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public ClientBuilder withAuthentications(List<Authentication> authentications) {
        this.authentications = authentications;
        return this;
    }

    public ClientBuilder withHttpBuilder(OkHttpClient.Builder okHttpBuilder){
        this.okHttpBuilder = okHttpBuilder;
        return this;
    }

    public DdiRestApi build(){
        final Interceptor interceptor = new AuthenticationRequestInterceptor(authentications);
        okHttpBuilder.interceptors().add(0, interceptor);
        return builder
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpBuilder.build())
                .build()
                .create(DdiRestApi.class);
    }

}
