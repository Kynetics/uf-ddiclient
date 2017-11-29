/*
 * Copyright © 2017 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.updatefactory.ddiclient.core;

import com.kynetics.updatefactory.ddiclient.api.ClientBuilder;
import com.kynetics.updatefactory.ddiclient.api.ServerType;
import com.kynetics.updatefactory.ddiclient.api.security.Authentication;
import com.kynetics.updatefactory.ddiclient.core.model.State;
import okhttp3.OkHttpClient;

import java.util.ArrayList;
import java.util.List;

import static com.kynetics.updatefactory.ddiclient.api.ServerType.HAWKBIT;
import static com.kynetics.updatefactory.ddiclient.api.security.Authentication.AuthenticationType.*;
import static com.kynetics.updatefactory.ddiclient.api.security.Authentication.newInstance;

/**
 * @author Daniele Sergio
 */
public class UFServiceBuilder {
    private String url;
    private String tenant;
    private String controllerId;
    private UFService.TargetData targetData;
    private State initialState = new State.WaitingState(0, null);
    private long retryDelayOnCommunicationError = 30_000;
    private OkHttpClient.Builder okHttpClientBuilder;
    private ServerType serverType = HAWKBIT;
    private List<Authentication> authentications = new ArrayList<>();

    UFServiceBuilder() {
        authentications.add(newInstance(ANONYMOUS_AUTHENTICATION,null));
    }

    public UFServiceBuilder withUrl(String url) {
        this.url = url;
        return this;
    }

    public UFServiceBuilder withGatewayToken(String token) {
        authentications.add(newInstance(GATEWAY_TOKEN_AUTHENTICATION, token));
        return this;
    }

    public UFServiceBuilder withTargetToken(String token) {
        authentications.add(newInstance(TARGET_TOKEN_AUTHENTICATION, token));
        return this;
    }

    public UFServiceBuilder withInitialState(State initialState) {
        this.initialState = initialState;
        return this;
    }

    public UFServiceBuilder withControllerId(String controllerId) {
        this.controllerId = controllerId;
        return this;
    }

    public UFServiceBuilder withTenant(String tenant) {
        this.tenant = tenant;
        return this;
    }

    public UFServiceBuilder withRetryDelayOnCommunicationError(long retryDelayOnCommunicationError) {
        this.retryDelayOnCommunicationError = retryDelayOnCommunicationError;
        return this;
    }

    public UFServiceBuilder withTargetData(UFService.TargetData targetData) {
        this.targetData = targetData;
        return this;
    }

    public UFServiceBuilder withOkHttClientBuilder(OkHttpClient.Builder okHttpClientBuilder) {
        this.okHttpClientBuilder = okHttpClientBuilder;
        return this;
    }

    public UFServiceBuilder withServerType(ServerType serverType){
       this.serverType = serverType;
       return this;
    }

    public UFService build() {
        validate(initialState, "initialState");
        validate(controllerId, "controllerId");
        validate(okHttpClientBuilder, "okHttpClientBuilder");
        validate(tenant, "tenant");
        validate(targetData, "targetData");
        validate(retryDelayOnCommunicationError, "retryDelayOnCommunicationError");
        validate(serverType, "serverType");
        final ClientBuilder clientBuilder = new ClientBuilder()
                .withHttpBuilder(okHttpClientBuilder)
                .withBaseUrl(url)
                .withServerType(serverType)
                .withAuthentications(authentications);
        return new UFService(clientBuilder.build(), tenant, controllerId, initialState, targetData, retryDelayOnCommunicationError);
    }

    private static void validate(String item, String itemName) {
        if (item == null || item.isEmpty()) {
            throw new IllegalStateException(String.format("%s could not be null or empty", itemName));
        }
    }

    private static void validate(Object item, String itemName) {
        if (item == null) {
            throw new IllegalStateException(String.format("%s could not be null", itemName));
        }
    }
}
