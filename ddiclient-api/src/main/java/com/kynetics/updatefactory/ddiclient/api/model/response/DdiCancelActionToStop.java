/*
 * Copyright © 2017 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.kynetics.updatefactory.ddiclient.api.model.response;

/**
 * The action that has to be stopped by the target.
 *
 * @author Daniele Sergio
 */
public class DdiCancelActionToStop {

    private String stopId;

    public String getStopId() {
        return stopId;
    }

    @Override
    public String toString() {
        return "CancelAction [stopId=" + stopId + "]";
    }

}
