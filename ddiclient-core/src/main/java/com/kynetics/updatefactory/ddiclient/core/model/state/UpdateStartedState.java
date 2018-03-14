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
import com.kynetics.updatefactory.ddiclient.core.model.event.UpdateErrorEvent;

import static com.kynetics.updatefactory.ddiclient.core.model.state.AbstractState.StateName.UPDATE_STARTED;

/**
 * @author Daniele Sergio
 */
public class UpdateStartedState extends AbstractStateWithAction {
    private static final long serialVersionUID = 2867815961297192784L;

    public UpdateStartedState(Long actionId) {
        super(UPDATE_STARTED, actionId);
    }

    @Override
    public AbstractState onEvent(AbstractEvent event) {
        switch (event.getEventName()) {
            case SUCCESS:
                return new UpdateEndedState(getActionId(), true, new String[0]);
            case UPDATE_ERROR:
                final UpdateErrorEvent errorEvent = (UpdateErrorEvent) event;
                return new UpdateEndedState(getActionId(), false, errorEvent.getDetails());
            default:
                return super.onEvent(event);
        }
    }
}
