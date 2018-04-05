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

import static com.kynetics.updatefactory.ddiclient.core.model.state.AbstractState.StateName.AUTHORIZATION_WAITING;

/**
 * @author Daniele Sergio
 */
public class AuthorizationWaitingState extends AbstractStateWithInnerState {

    private static final long serialVersionUID = -600230948021324873L;

    private transient boolean requestSend;

    public AuthorizationWaitingState(AbstractState state) {
        super(AUTHORIZATION_WAITING, state);
        requestSend = false;
    }

    @Override
    public AbstractState onEvent(AbstractEvent event) {
        switch (event.getEventName()) {
            case AUTHORIZATION_WAITING:
                return this;
            case AUTHORIZATION_GRANTED:
                return getState();
            case CANCEL:
                return  new CancellationCheckState(this, ((CancelEvent) event).getActionId());
            case AUTHORIZATION_DENIED:
                return new WaitingState(30_000, (AbstractStateWithAction) getState()); // FIXME: 9/11/17 sleeptime should be the last sleeptime found
            default:
                return super.onEvent(event);
        }
    }

    public boolean isRequestSend() {
        return requestSend;
    }

    public void sendRequest() {
        this.requestSend = true;
    }

}
