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

import java.util.Collections;
import java.util.List;

/**
 * @author Daniele Sergio
 */
public abstract class AbstractStateWithFile extends AbstractUpdateState {

    private static final long serialVersionUID = 2394941454351751966L;

    private final List<FileInfo> fileInfoList;
    private final int nextFileToDownload;
    private final Hash lastHash;

    public AbstractStateWithFile(StateName stateName, Long actionId, boolean isForced, List<FileInfo> fileInfoList, int nextFileToDownload, Hash lastHash) {
        super(stateName, actionId, isForced);
        this.fileInfoList = fileInfoList;
        this.nextFileToDownload = nextFileToDownload;
        this.lastHash = lastHash;
    }

    public FileInfo getFileInfo() {
        return fileInfoList.get(nextFileToDownload);
    }

    public int getSize() {
        return fileInfoList.size();
    }

    public int getNextFileToDownload() {
        return nextFileToDownload;
    }

    public List<FileInfo> getFileInfoList() {
        return Collections.unmodifiableList(fileInfoList);
    }

    public Hash getLastHash() {
        return lastHash;
    }
}
