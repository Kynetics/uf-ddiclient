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
public abstract class AbstractUpdateState extends AbstractStateWithAction {
    private static final long serialVersionUID = -2826703270286073179L;

    private final boolean isForced;

    public AbstractUpdateState(StateName stateName, Long actionId, boolean isForced) {
        super(stateName, actionId);
        this.isForced = isForced;
    }

    public boolean isForced() {
        return isForced;
    }
}
