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

import com.kynetics.updatefactory.ddiclient.core.model.event.*;

import java.io.Serializable;

import static com.kynetics.updatefactory.ddiclient.core.model.state.AbstractCommunicationState.MAX_ATTEMPTS;

/**
 * @author Daniele Sergio
 */
public abstract class AbstractState implements Serializable{
    private static final long serialVersionUID = 7711303489333786769L;

    public enum StateName{
        WAITING, CONFIG_DATA, UPDATE_INITIALIZATION, UPDATE_DOWNLOAD, SAVING_FILE, UPDATE_READY, UPDATE_STARTED, CANCELLATION_CHECK,
        CANCELLATION, UPDATE_ENDED, COMMUNICATION_FAILURE, COMMUNICATION_ERROR, AUTHORIZATION_WAITING, SERVER_FILE_CORRUPTED
    }

    private final StateName stateName;
    public AbstractState(StateName stateName) {
        this.stateName = stateName;
    }

    public StateName getStateName() {
        return stateName;
    }

    public AbstractState onEvent(AbstractEvent event){
        switch (event.getEventName()){
            case ERROR:
                ErrorEvent errorEvent = (ErrorEvent) event;
                return getStateOnError(errorEvent, this, MAX_ATTEMPTS);
            case FAILURE:
                FailureEvent failureEvent = (FailureEvent) event;
                return new CommunicationFailureState(this,failureEvent.getThrowable());
            default:
                throw new IllegalStateException(String.format("AbstractEvent %s not handler in %s state", event.getEventName(), stateName));
        }
    }

    static AbstractState getStateOnError(ErrorEvent errorEvent, AbstractState state, int retry) {
        return errorEvent.getCode() == 404 && errorEvent.getDetails()[0] != null &&
                errorEvent.getDetails()[0].equals("hawkbit.server.error.repo.entitiyNotFound") ?
                new WaitingState(0,null) :
                new CommunicationErrorState(state,retry, errorEvent.getCode(), errorEvent.getDetails());
    }


}
