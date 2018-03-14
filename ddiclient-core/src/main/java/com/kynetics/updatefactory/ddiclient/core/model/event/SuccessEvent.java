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

import static com.kynetics.updatefactory.ddiclient.core.model.event.AbstractEvent.EventName.SUCCESS;

/**
 * @author Daniele Sergio
 */
public class SuccessEvent extends AbstractEventWithActionId {

    private static final long serialVersionUID = -6072947302760156164L;

    public SuccessEvent() {
        super(SUCCESS, null);
    }

    public SuccessEvent(Long actionId) {
        super(SUCCESS, actionId);
    }
}
