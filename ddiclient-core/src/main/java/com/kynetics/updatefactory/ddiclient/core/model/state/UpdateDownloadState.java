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

import com.kynetics.updatefactory.ddiclient.core.model.FileInfo;
import com.kynetics.updatefactory.ddiclient.core.model.Hash;
import com.kynetics.updatefactory.ddiclient.core.model.event.AbstractEvent;
import com.kynetics.updatefactory.ddiclient.core.model.event.DownloadStartedEvent;

import java.util.List;

import static com.kynetics.updatefactory.ddiclient.core.model.state.AbstractState.StateName.UPDATE_DOWNLOAD;

/**
 * @author Daniele Sergio
 */
public class UpdateDownloadState extends AbstractStateWithFile {
    private static final long serialVersionUID = -1998879559588928971L;


    public UpdateDownloadState(Long actionId,
                               boolean isForced,
                               List<FileInfo> fileInfoList,
                               int nextFileToDownload,
                               Hash lastHash) {
        super(UPDATE_DOWNLOAD, actionId, isForced, fileInfoList, nextFileToDownload, lastHash);
    }

    public UpdateDownloadState(Long actionId, boolean isForced, List<FileInfo> fileInfoList, int nextFileToDownload) {
        this(actionId, isForced, fileInfoList, nextFileToDownload, null);
    }

    @Override
    public AbstractState onEvent(AbstractEvent event) {
        switch (event.getEventName()) {
            case DOWNLOAD_STARTED:
                return new SavingFileState(getActionId(), isForced(), getFileInfoList(), getNextFileToDownload(), getLastHash(), ((DownloadStartedEvent) event).getInputStream());
            default:
                return super.onEvent(event);
        }
    }
}
