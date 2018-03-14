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

import com.kynetics.updatefactory.ddiclient.core.model.Hash;

import static com.kynetics.updatefactory.ddiclient.core.model.event.AbstractEvent.EventName.FILE_CORRUPTED;

/**
 * @author Daniele Sergio
 */
public class FileCorruptedEvent extends AbstractEvent {

    private static final long serialVersionUID = -1474400707830527730L;

    private final Hash downloadedFileHash;

    public FileCorruptedEvent(Hash downloadedFileHash) {
        super(FILE_CORRUPTED);
        this.downloadedFileHash = downloadedFileHash;
    }

    public Hash getDownloadedFileHash() {
        return downloadedFileHash;
    }
}
