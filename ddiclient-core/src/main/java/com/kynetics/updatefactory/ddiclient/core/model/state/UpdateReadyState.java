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

import static com.kynetics.updatefactory.ddiclient.core.model.state.AbstractState.StateName.UPDATE_READY;

/**
 * @author Daniele Sergio
 */
public class UpdateReadyState extends AbstractUpdateState {
    private static final long serialVersionUID = -8501350119987754124L;

    public UpdateReadyState(Long actionId, boolean isForced) {
        super(UPDATE_READY, actionId, isForced);
    }

    @Override
    public AbstractState onEvent(AbstractEvent event) {
        switch (event.getEventName()) {
            case CANCEL:
                return new CancellationCheckState(this, ((CancelEvent) event).getActionId());
            case SUCCESS:
                final AbstractStateWithAction state = new UpdateStartedState(getActionId());
                return isForced() ? state : new AuthorizationWaitingState(state);
            default:
                return super.onEvent(event);
        }
    }
}
