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

import static com.kynetics.updatefactory.ddiclient.core.model.state.AbstractState.StateName.COMMUNICATION_ERROR;

/**
 * @author Daniele Sergio
 */
public class CommunicationErrorState extends AbstractCommunicationState {
    private static final long serialVersionUID = 6055896033449978948L;

    private final int code;
    private final String[] details;

    public CommunicationErrorState(AbstractState state, int retry, int code, String[] details) {
        super(COMMUNICATION_ERROR, state, retry);
        this.code = code;
        this.details = details;
    }

    public CommunicationErrorState(AbstractState state, int code, String[] details) {
        this(state, MAX_ATTEMPTS, code, details);
    }

    public long getCode() {
        return code;
    }

    public String[] getDetails() {
        return details.clone();
    }
}
