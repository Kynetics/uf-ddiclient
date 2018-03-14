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

/**
 * @author Daniele Sergio
 */
public abstract class AbstractStateWithInnerState extends AbstractState {
    private static final long serialVersionUID = -4406678501074253705L;

    private final AbstractState state;

    public AbstractStateWithInnerState(StateName stateName, AbstractState state) {
        super(stateName);
        this.state = state;
    }

    public AbstractState getState() {
        return state;
    }

    public boolean hasInnerState() {
        return state != null;
    }

}
