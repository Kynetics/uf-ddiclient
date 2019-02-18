/*
 * Copyright © 2017-2018 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.updatefactory.ddiclient.core;

import com.kynetics.updatefactory.ddiclient.api.api.DdiRestApi;
import com.kynetics.updatefactory.ddiclient.core.model.state.AbstractState;
import com.kynetics.updatefactory.ddiclient.core.model.state.WaitingState;
import com.kynetics.updatefactory.ddiclient.core.servicecallback.SystemOperation;
import com.kynetics.updatefactory.ddiclient.core.servicecallback.UserInteraction;

import static com.kynetics.updatefactory.ddiclient.api.validation.Assert.NotEmpty;
import static com.kynetics.updatefactory.ddiclient.api.validation.Assert.NotNull;

/**
 * @author Daniele Sergio
 */
public class UFServiceBuilder {
    private String tenant;
    private String controllerId;
    private UFService.TargetData targetData;
    private AbstractState initialState = new WaitingState(0, null);
    private long retryDelayOnCommunicationError = 30_000;
    private DdiRestApi client;
    private SystemOperation systemOperation;
    private UserInteraction userInteraction;

    UFServiceBuilder() { }


    public UFServiceBuilder withInitialState(AbstractState initialState) {
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

    public UFServiceBuilder withSystemOperation(SystemOperation systemOperation){
        this.systemOperation = systemOperation;
        return this;
    }

    public UFServiceBuilder withUserInteraction(UserInteraction userInteraction){
        this.userInteraction = userInteraction;
        return this;
    }

    public UFService build() {
        NotNull(initialState, "initialState");
        NotEmpty(controllerId, "controllerId");
        NotEmpty(tenant, "tenant");
        NotNull(targetData, "targetData");
        NotNull(retryDelayOnCommunicationError, "retryDelayOnCommunicationError");
        NotNull(client, "client");
        NotNull(systemOperation, "systemOperation");
        NotNull(userInteraction, "userInteraction");
        return new UFService(client,
                tenant,
                controllerId,
                initialState,
                targetData,
                systemOperation,
                userInteraction,
                Math.max(retryDelayOnCommunicationError, 30_000));
    }
}
