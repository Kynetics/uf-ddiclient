/*
 * Copyright © 2017 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.updatefactory.core;

import com.kynetics.updatefactory.core.model.State;

/**
 * @author Daniele Sergio
 */
public class UFServiceBuilder {
    private String url;
    private String username;
    private String password;
    private String tenant;
    private String controllerId;
    private UFService.TargetData targetData;
    private State initialState = new State.WaitingState(0, null);
    private long retryDelayOnCommunicationError = 30_000;

    UFServiceBuilder() {
    }

    public UFServiceBuilder withUrl(String url) {
        this.url = url;
        return this;
    }

    public UFServiceBuilder withUsername(String username) {
        this.username = username;
        return this;
    }

    public UFServiceBuilder withPassword(String password) {
        this.password = password;
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

    public UFService build() {
        validate(url, "url");
        validate(username, "username");
        validate(password, "password");
        validate(initialState, "initialState");
        validate(controllerId, "controllerId");
        validate(tenant, "tenant");
        validate(targetData, "targetData");
        validate(retryDelayOnCommunicationError, "retryDelayOnCommunicationError");
        return new UFService(url, username, password, tenant, controllerId, initialState, targetData, retryDelayOnCommunicationError);
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

    private static void validate(long item, String itemName) {
        if (item < 1_000) {
            throw new IllegalStateException(String.format("%s must be bigger than 999", itemName));
        }
    }
}
