/*
 * Copyright © 2017-2018 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.updatefactory.ddiclient.core.model.state;

import com.kynetics.updatefactory.ddiclient.core.model.event.AbstractEvent;
import com.kynetics.updatefactory.ddiclient.core.model.event.ErrorEvent;
import com.kynetics.updatefactory.ddiclient.core.model.event.FailureEvent;

import static com.kynetics.updatefactory.ddiclient.core.model.state.AbstractState.StateName.UPDATE_DOWNLOAD;

/**
 * @author Daniele Sergio
 */
public abstract class AbstractCommunicationState extends AbstractStateWithInnerState {

    static final int MAX_ATTEMPTS = 5;

    private static final long serialVersionUID = -4069915532113945570L;

    private final int attemptsRemaining;

    public AbstractCommunicationState(StateName stateName, AbstractState state, int attemptsRemaining) {
        super(stateName, state);
        if (state != null && state.getStateName().equals(UPDATE_DOWNLOAD)) {
            this.attemptsRemaining = attemptsRemaining;
        } else {
            this.attemptsRemaining = MAX_ATTEMPTS;
        }
    }

    @Override
    public AbstractState onEvent(AbstractEvent event) {
        switch (event.getEventName()) {
            case ERROR:
                final ErrorEvent errorEvent = (ErrorEvent) event;
                return attemptsRemaining == 0 ? new WaitingState(0, this) : getStateOnError(errorEvent, getState(), attemptsRemaining - 1);
            case FAILURE:
                FailureEvent failureEvent = (FailureEvent) event;
                return attemptsRemaining == 0 ? new WaitingState(0, this) : new CommunicationFailureState(getState(), attemptsRemaining - 1, failureEvent.getThrowable());
            default:
                return getState().onEvent(event);
        }
    }
}
