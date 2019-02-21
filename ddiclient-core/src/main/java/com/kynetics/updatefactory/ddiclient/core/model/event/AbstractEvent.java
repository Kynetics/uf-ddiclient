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

import java.io.Serializable;

/**
 * @author Daniele Sergio
 */
public abstract class AbstractEvent implements Serializable{

    private static final long serialVersionUID = 3311499215842422409L;

    public enum EventName{
        SLEEP_REQUEST,  UPDATE_CONFIG_REQUEST, SUCCESS, FAILURE, ERROR, UPDATE_FOUND, DOWNLOAD_REQUEST, DOWNLOAD_STARTED,
        FILE_CORRUPTED, CANCEL, UPDATE_ERROR, AUTHORIZATION_GRANTED, AUTHORIZATION_DENIED, RESUME, DOWNLOAD_PENDING,
        FORCE_CANCEL, AUTHORIZATION_PENDING, INSUFFICIENT_SPACE
    }

    private final EventName eventName;

    public AbstractEvent(EventName eventName) {
        this.eventName = eventName;
    }

    public EventName getEventName() {
        return eventName;
    }

}
