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

import com.kynetics.updatefactory.ddiclient.api.model.response.DdiDeploymentBase;

import static com.kynetics.updatefactory.ddiclient.core.model.event.AbstractEvent.EventName.DOWNLOAD_REQUEST;

/**
 * @author Daniele Sergio
 */
public class DownloadRequestEvent extends AbstractEvent {

    private static final long serialVersionUID = -6848816964820611672L;

    private final DdiDeploymentBase ddiDeploymentBase;

    public DownloadRequestEvent(DdiDeploymentBase ddiDeploymentBase) {
        super(DOWNLOAD_REQUEST);
        this.ddiDeploymentBase = ddiDeploymentBase;
    }

    public DdiDeploymentBase getDdiDeploymentBase() {
        return ddiDeploymentBase;
    }
}
