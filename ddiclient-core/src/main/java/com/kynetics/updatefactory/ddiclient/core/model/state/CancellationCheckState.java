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

import static com.kynetics.updatefactory.ddiclient.core.model.state.AbstractState.StateName.*;

/**
 * @author Daniele Sergio
 */
public class CancellationCheckState extends AbstractStateWithAction {
    private static final long serialVersionUID = -8085857299755753381L;

    private final AbstractState previousState;

    public CancellationCheckState(AbstractState previousState, Long actionId) {
        super(CANCELLATION_CHECK, actionId);
        this.previousState = previousState;
    }

    @Override
    public AbstractState onEvent(AbstractEvent event) {
        switch (event.getEventName()) {
            case SUCCESS: //must cancel the action into the event (successEvent.getActionId()) but I need to send the feedback to the action inside the nextFileToDownload state (getAction);
                final StateName stateName = getPreviousState().getStateName();
                final SuccessEvent successEvent = (SuccessEvent) event;
                if (stateName == UPDATE_READY) {
                    final UpdateReadyState updateReadyState = (UpdateReadyState) getPreviousState();
                    return updateReadyState.getActionId() == successEvent.getActionId() ?
                            new CancellationState(getActionId()) :
                            updateReadyState.isForced() ?
                                    new UpdateStartedState(updateReadyState.getActionId()) :
                                    new AuthorizationWaitingState(updateReadyState);
                } else if (stateName == SAVING_FILE){
                    final SavingFileState savingFileState = (SavingFileState) getPreviousState();
                    return  savingFileState.getActionId() == successEvent.getActionId() ?
                            new CancellationState(getActionId()) : savingFileState;
                } else if (stateName == AUTHORIZATION_WAITING){
                    final AuthorizationWaitingState authorizationWaitingState = (AuthorizationWaitingState) getPreviousState();
                    final AbstractState innerState = authorizationWaitingState.getState();
                    final long innerStateId = innerState instanceof AbstractStateWithAction ?
                            ((AbstractStateWithAction) innerState).getActionId() : -1;
                    return innerStateId == successEvent.getActionId() ?
                            new CancellationState(getActionId()) : authorizationWaitingState;
                }
                return new CancellationState(getActionId());
            case CANCEL:
                return this;
            default:
                return super.onEvent(event);
        }
    }

    public AbstractState getPreviousState() {
        return previousState;
    }
}
