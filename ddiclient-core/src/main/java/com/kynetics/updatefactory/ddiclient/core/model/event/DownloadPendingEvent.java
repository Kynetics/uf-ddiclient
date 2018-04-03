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

import static com.kynetics.updatefactory.ddiclient.core.model.event.AbstractEvent.EventName.DOWNLOAD_PENDING;

/**
 * @author Daniele Sergio
 */
public class DownloadPendingEvent extends AbstractEvent{

    private static final long serialVersionUID = -5300504162554660122L;

    public DownloadPendingEvent() {
        super(DOWNLOAD_PENDING);
    }
}
