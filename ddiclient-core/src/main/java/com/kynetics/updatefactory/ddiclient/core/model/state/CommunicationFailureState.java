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

import static com.kynetics.updatefactory.ddiclient.core.model.state.AbstractState.StateName.COMMUNICATION_FAILURE;

/**
 * @author Daniele Sergio
 */
public class CommunicationFailureState extends AbstractCommunicationState {

    private static final long serialVersionUID = 7696010738651506431L;

    private final Throwable throwable;

    public CommunicationFailureState(AbstractState state, int retry, Throwable throwable) {
        super(COMMUNICATION_FAILURE, state, retry);
        this.throwable = throwable;
    }

    public CommunicationFailureState(AbstractState state, Throwable throwable) {
        this(state, MAX_ATTEMPTS, throwable);
    }

    public Throwable getThrowable() {
        return throwable;
    }

}
