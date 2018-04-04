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

import java.io.InputStream;

import static com.kynetics.updatefactory.ddiclient.core.model.event.AbstractEvent.EventName.DOWNLOAD_STARTED;

/**
 * @author Daniele Sergio
 */
public class DownloadStartedEvent extends AbstractEvent {

    private static final long serialVersionUID = 3952484262164404297L;
    private final transient InputStream inputStream;
    private final String fileName;

    public DownloadStartedEvent(InputStream inputStream,
                                String fileName) {
        super(DOWNLOAD_STARTED);
        this.inputStream = inputStream;
        this.fileName = fileName;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public String getFileName() {
        return fileName;
    }
}
