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
import com.kynetics.updatefactory.ddiclient.core.model.event.CancelEvent;
import com.kynetics.updatefactory.ddiclient.core.model.event.SleepEvent;
import com.kynetics.updatefactory.ddiclient.core.model.event.UpdateFoundEvent;

import static com.kynetics.updatefactory.ddiclient.core.model.state.AbstractState.StateName.COMMUNICATION_ERROR;
import static com.kynetics.updatefactory.ddiclient.core.model.state.AbstractState.StateName.COMMUNICATION_FAILURE;
import static com.kynetics.updatefactory.ddiclient.core.model.state.AbstractState.StateName.WAITING;

/**
 * @author Daniele Sergio
 */
public class WaitingState extends AbstractStateWithInnerState {
    private static final long serialVersionUID = -8905024383731749954L;

    private final long sleepTime;

    public WaitingState(long sleepTime, AbstractState suspendState) {
        super(WAITING, suspendState);
        this.sleepTime = sleepTime;
    }

    public long getSleepTime() {
        return sleepTime;
    }

    @Override
    public AbstractState onEvent(AbstractEvent event) {
        switch (event.getEventName()) {
            case SLEEP_REQUEST:
                return new com.kynetics.updatefactory.ddiclient.core.model.state.WaitingState(((SleepEvent) event).getSleepTime(), getState());
            case UPDATE_CONFIG_REQUEST:
                return new ConfigDataState();
            case UPDATE_FOUND:
                final UpdateFoundEvent updateFoundEvent = (UpdateFoundEvent) event;
                return hasInnerState() && updateFoundEvent.getActionId().equals(getInnerStateActionId()) ?
                        this :
                        new UpdateInitialization(((UpdateFoundEvent) event).getActionId());
            case CANCEL:
                return new CancellationCheckState(this, ((CancelEvent) event).getActionId());
            case RESUME:
                return innerStateIsCommunicationState() ?
                        getMostInnerState() :
                        new AuthorizationWaitingState(getState());
            default:
                return super.onEvent(event);
        }

    }

    public boolean innerStateIsCommunicationState() {
        if (!hasInnerState()) {
            return false;
        }
        final StateName innerStateName = getState().getStateName();
        return innerStateName == COMMUNICATION_ERROR || innerStateName == COMMUNICATION_FAILURE;
    }

    private AbstractState getMostInnerState() {
        return innerStateIsCommunicationState() ? ((AbstractCommunicationState) getState()).getState() : getState();
    }

    private Long getInnerStateActionId() {
        final AbstractState state = getMostInnerState();
        return state instanceof AbstractStateWithAction ?
                ((AbstractStateWithAction) state).getActionId() :
                null;
    }
}
