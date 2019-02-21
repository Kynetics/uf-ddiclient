/*
 * Copyright © 2017-2018 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.updatefactory.ddiclient.example.callback;

import com.kynetics.updatefactory.ddiclient.core.model.FileInfo;
import com.kynetics.updatefactory.ddiclient.core.servicecallback.SystemOperation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * @author Daniele Sergio
 */
public class SystemOperationMock implements SystemOperation {

    private UpdateStatus status = UpdateStatus.newPendingStatus();

    @Override
    public boolean savingFile(InputStream inputStream, FileInfo fileInfo) {
        try {
            Files.copy(
                    inputStream,
                    new File(fileInfo.getLinkInfo().getFileName()).toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void executeUpdate(long actionId) {
        status = UpdateStatus.newSuccessStatus(null);
    }

    @Override
    public UpdateStatus updateStatus() {
        return status;
    }

    @Override
    public boolean checkSpace(long spaceNeeded) {
        return true;
    }
}
