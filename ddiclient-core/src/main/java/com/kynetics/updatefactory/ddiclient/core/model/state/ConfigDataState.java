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

import static com.kynetics.updatefactory.ddiclient.core.model.state.AbstractState.StateName.CONFIG_DATA;

/**
 * @author Daniele Sergio
 */
public class ConfigDataState extends AbstractState {
    private static final long serialVersionUID = 1510486692091734525L;

    public ConfigDataState() {
        super(CONFIG_DATA);
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
}
