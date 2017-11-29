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

import com.kynetics.updatefactory.ddiclient.api.api.DdiRestApi;
import com.kynetics.updatefactory.ddiclient.core.model.State;

/**
 * @author Daniele Sergio
 */
public class UFServiceBuilder {
    private String tenant;
    private String controllerId;
    private UFService.TargetData targetData;
    private State initialState = new State.WaitingState(0, null);
    private long retryDelayOnCommunicationError = 30_000;
    private DdiRestApi client;

    UFServiceBuilder() { }


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

    public UFServiceBuilder withClient(DdiRestApi ddiRestApi){
        this.client = ddiRestApi;
        return this;
    }

    public UFService build() {
        validate(initialState, "initialState");
        validate(controllerId, "controllerId");
        validate(tenant, "tenant");
        validate(targetData, "targetData");
        validate(retryDelayOnCommunicationError, "retryDelayOnCommunicationError");
        validate(client, "client");
        return new UFService(client,
                tenant,
                controllerId,
                initialState,
                targetData,
                retryDelayOnCommunicationError);
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
