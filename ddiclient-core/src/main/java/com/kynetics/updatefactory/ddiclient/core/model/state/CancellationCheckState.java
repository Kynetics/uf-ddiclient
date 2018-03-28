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
import com.kynetics.updatefactory.ddiclient.core.model.event.SuccessEvent;

import static com.kynetics.updatefactory.ddiclient.core.model.state.AbstractState.StateName.CANCELLATION_CHECK;
import static com.kynetics.updatefactory.ddiclient.core.model.state.AbstractState.StateName.UPDATE_READY;

/**
 * @author Daniele Sergio
 */
public class CancellationCheckState extends AbstractStateWithAction {
    private static final long serialVersionUID = 7842141220101864204L;

    private final AbstractState previousState;

    public CancellationCheckState(AbstractState previousState, Long actionId) {
        super(CANCELLATION_CHECK, actionId);
        this.previousState = previousState;
    }

    @Override
    public AbstractState onEvent(AbstractEvent event) {
        switch (event.getEventName()) {
            case SUCCESS: //must cancel the action into the event (successEvent.getActionId()) but I need to send the feedback to the action inside the nextFileToDownload state (getAction);
                if (getPreviousState().getStateName() == UPDATE_READY) {
                    final SuccessEvent successEvent = (SuccessEvent) event;
                    final UpdateReadyState updateReadyState = (UpdateReadyState) getPreviousState();
                    return updateReadyState.getActionId() == successEvent.getActionId() ?
                            new CancellationState(getActionId()) :
                            updateReadyState.isForced() ?
                                    new UpdateStartedState(updateReadyState.getActionId()) :
                                    new AuthorizationWaitingState(updateReadyState);
                }
                return new CancellationState(getActionId());
            default:
                return super.onEvent(event);
        }
    }

    public AbstractState getPreviousState() {
        return previousState;
    }
}
