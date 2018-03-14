/*
 * Copyright © 2017-2018 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.updatefactory.ddiclient.core.model.event;

/**
 * @author Daniele Sergio
 */
public abstract class AbstractEventWithActionId extends AbstractEvent {

    private static final long serialVersionUID = -1216377107118471482L;

    private final Long actionId;

    public AbstractEventWithActionId(EventName eventName, Long actionId) {
        super(eventName);
        this.actionId = actionId;
    }

    public Long getActionId() {
        return actionId;
    }
}
