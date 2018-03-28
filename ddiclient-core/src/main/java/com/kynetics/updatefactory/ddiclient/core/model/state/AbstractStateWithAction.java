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
public abstract class AbstractStateWithAction extends AbstractState {
    private static final long serialVersionUID = 7423883109789941001L;

    private final long actionId;

    public AbstractStateWithAction(StateName stateName, long actionId) {
        super(stateName);
        this.actionId = actionId;
    }

    public long getActionId() {
        return actionId;
    }
}
