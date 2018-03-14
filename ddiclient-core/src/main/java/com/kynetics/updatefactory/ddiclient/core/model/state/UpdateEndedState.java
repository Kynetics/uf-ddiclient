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

import static com.kynetics.updatefactory.ddiclient.core.model.state.AbstractState.StateName.UPDATE_ENDED;

/**
 * @author Daniele Sergio
 */
public class UpdateEndedState extends AbstractStateWithAction {
    private static final long serialVersionUID = -868250366352211123L;

    private final boolean isSuccessfullyUpdate;
    private final String[] details;

    public UpdateEndedState(Long actionId, boolean isSuccessfullyUpdate, String[] details) {
        super(UPDATE_ENDED, actionId);
        this.isSuccessfullyUpdate = isSuccessfullyUpdate;
        this.details = details;
    }

    @Override
    public AbstractState onEvent(AbstractEvent event) {
        switch (event.getEventName()) {
            case SUCCESS:
                return new WaitingState(0, null);
            default:
                return super.onEvent(event);
        }
    }

    public boolean isSuccessfullyUpdate() {
        return isSuccessfullyUpdate;
    }

    public String[] getDetails() {
        return details;
    }
}
