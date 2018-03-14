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

import static com.kynetics.updatefactory.ddiclient.core.model.event.AbstractEvent.EventName.SLEEP_REQUEST;

/**
 * @author Daniele Sergio
 */
public class SleepEvent extends AbstractEvent {

    private static final long serialVersionUID = -2879957856504030860L;

    final long sleepTime;

    public SleepEvent(long sleepTime) {
        super(SLEEP_REQUEST);
        this.sleepTime = sleepTime;
    }

    public long getSleepTime() {
        return sleepTime;
    }
}
