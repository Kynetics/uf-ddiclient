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

import com.kynetics.updatefactory.ddiclient.api.model.response.DdiArtifact;
import com.kynetics.updatefactory.ddiclient.api.model.response.DdiChunk;
import com.kynetics.updatefactory.ddiclient.core.model.FileInfo;
import com.kynetics.updatefactory.ddiclient.core.model.Hash;
import com.kynetics.updatefactory.ddiclient.core.model.event.AbstractEvent;
import com.kynetics.updatefactory.ddiclient.core.model.event.DownloadRequestEvent;

import java.util.ArrayList;
import java.util.List;

import static com.kynetics.updatefactory.ddiclient.api.model.response.DdiDeployment.HandlingType.FORCED;
import static com.kynetics.updatefactory.ddiclient.core.model.state.AbstractState.StateName.UPDATE_INITIALIZATION;

/**
 * @author Daniele Sergio
 */
public class UpdateInitialization extends AbstractStateWithAction {

    private static final long serialVersionUID = -703356064985341858L;

    public UpdateInitialization(Long actionId) {
        super(UPDATE_INITIALIZATION, actionId);
    }

    @Override
    public AbstractState onEvent(AbstractEvent event) {
        switch (event.getEventName()) {
            case DOWNLOAD_REQUEST:
                final DownloadRequestEvent downloadRequestEvent = ((DownloadRequestEvent) event);
                final List<FileInfo> fileInfoList = new ArrayList<>();
                for (DdiChunk chunk : downloadRequestEvent.getDdiDeploymentBase().getDeployment().getChunks()) {
                    for (DdiArtifact artifact : chunk.getArtifacts()) {
                        fileInfoList.add(new FileInfo(
                                artifact.getLink("download-http").parseLink2(),
                                new Hash(artifact.getHashes().getMd5(),
                                        artifact.getHashes().getSha1())));
                    }
                }
                final boolean isForced = downloadRequestEvent.getDdiDeploymentBase().getDeployment().getDownload() == FORCED;
                final boolean noFile = fileInfoList.size() == 0;
                final AbstractState state = noFile ?
                        new UpdateEndedState(getActionId(), true, new String[]{"Update doesn't have file"}) :
                        new UpdateDownloadState(getActionId(), isForced, fileInfoList, 0);
                return isForced || noFile ? state : new AuthorizationWaitingState(state);

            default:
                return super.onEvent(event);
        }
    }
}
