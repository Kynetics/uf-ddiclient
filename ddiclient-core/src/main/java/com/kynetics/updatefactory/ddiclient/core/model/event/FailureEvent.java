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

import static com.kynetics.updatefactory.ddiclient.core.model.event.AbstractEvent.EventName.FAILURE;

/**
 * @author Daniele Sergio
 */
public class FailureEvent extends AbstractEvent {

    private static final long serialVersionUID = 4328037593651625818L;

    private final Throwable throwable;

    public FailureEvent(Throwable throwable) {
        super(FAILURE);
        this.throwable = throwable;
    }

    public Throwable getThrowable() {
        return throwable;
    }
}
