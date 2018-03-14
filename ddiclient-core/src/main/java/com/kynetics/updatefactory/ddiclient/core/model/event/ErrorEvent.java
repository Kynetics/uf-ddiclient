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

import static com.kynetics.updatefactory.ddiclient.core.model.event.AbstractEvent.EventName.ERROR;

/**
 * @author Daniele Sergio
 */
public class ErrorEvent extends AbstractEvent {

    private static final long serialVersionUID = -5155573189318265338L;
    private final String[] details;
    private final int code;

    public ErrorEvent(String[] details, int code) {
        super(ERROR);
        this.details = details;
        this.code = code;
    }

    public String[] getDetails() {
        return details;
    }

    public int getCode() {
        return code;
    }
}
