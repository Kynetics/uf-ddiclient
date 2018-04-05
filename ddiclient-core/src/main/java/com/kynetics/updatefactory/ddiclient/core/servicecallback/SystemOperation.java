/*
 * Copyright © 2017-2018 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.updatefactory.ddiclient.core.servicecallback;

import com.kynetics.updatefactory.ddiclient.core.model.FileInfo;

import java.io.InputStream;

/**
 * @author Daniele Sergio
 */
public interface SystemOperation {
    boolean savingFile(InputStream inputStream, FileInfo fileInfo);

    void executeUpdate(long actionId);

    UpdateStatus updateStatus();

    enum UpdateStatus {
        NOT_APPLIED, APPLIED_WITH_ERROR, SUCCESSFULLY_APPLIED
    }
}
