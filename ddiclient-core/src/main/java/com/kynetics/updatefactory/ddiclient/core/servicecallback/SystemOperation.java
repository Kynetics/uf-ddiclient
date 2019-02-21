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

    boolean checkSpace(long spaceNeeded);

    class UpdateStatus{
        private final StatusName statusName;
        private final String[] messages;

        public static UpdateStatus newSuccessStatus(String[] details){
            return new UpdateStatus(StatusName.SUCCESSFULLY_APPLIED, getMessages(details));
        }

        public static UpdateStatus newFailureStatus(String[] details){
            return new UpdateStatus(StatusName.APPLIED_WITH_ERROR, getMessages(details));
        }

        public static UpdateStatus newPendingStatus(){
            return new UpdateStatus(StatusName.NOT_APPLIED, getMessages(null));
        }

        private static String[] getMessages(String[] messages) {
            return messages == null ? new String[0] : messages;
        }

        UpdateStatus(StatusName statusName, String[] details) {
            this.statusName = statusName;
            this.messages = details;
        }

        public StatusName getStatusName() {
            return statusName;
        }

        public String[] getMessages() {
            return messages;
        }
    }

    enum StatusName {
        NOT_APPLIED, APPLIED_WITH_ERROR, SUCCESSFULLY_APPLIED
    }
}
