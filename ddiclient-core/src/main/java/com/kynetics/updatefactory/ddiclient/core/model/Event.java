/*
 * Copyright © 2017 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.updatefactory.ddiclient.core.model;

import com.kynetics.updatefactory.ddiclient.api.model.response.DdiDeploymentBase;

import java.io.InputStream;
import java.io.Serializable;

import static com.kynetics.updatefactory.ddiclient.core.model.Event.EventName.*;

/**
 * @author Daniele Sergio
 */
public class Event implements Serializable{

    private static final long serialVersionUID = -379773284801159482L;

    public enum EventName{
        SLEEP_REQUEST,  UPDATE_CONFIG_REQUEST, SUCCESS, FAILURE, ERROR, UPDATE_FOUND, DOWNLOAD_REQUEST, FILE_DOWNLOADED,
        FILE_CORRUPTED, CANCEL, UPDATE_ERROR, AUTHORIZATION_GRANTED, AUTHORIZATION_DENIED, RESUME
    }

    private final EventName eventName;

    public Event(EventName eventName) {
        this.eventName = eventName;
    }

    public EventName getEventName() {
        return eventName;
    }

    public static class SleepEvent extends Event{

        private static final long serialVersionUID = -2879957856504030860L;

        final long sleepTime;

        public SleepEvent(long sleepTime) {
            super(SLEEP_REQUEST);
            this.sleepTime = sleepTime;
        }

        public long getSleepTime() {
            return sleepTime;
        }
    }

    public static class FailureEvent extends Event{

        private static final long serialVersionUID = 4328037593651625818L;

        private final Throwable throwable;
        public FailureEvent(Throwable throwable) {
            super(FAILURE);
            this.throwable = throwable;
        }

        public Throwable getThrowable() {
            return throwable;
        }
    }

    public static abstract class EventWithActionId extends Event{

        private static final long serialVersionUID = -1216377107118471482L;

        private final Long actionId;

        public EventWithActionId(EventName eventName, Long actionId) {
            super(eventName);
            this.actionId = actionId;
        }

        public Long getActionId() {
            return actionId;
        }
    }

    public static class SuccessEvent extends EventWithActionId{

        private static final long serialVersionUID = -6072947302760156164L;

        public SuccessEvent() {
            super(SUCCESS,null);
        }

        public SuccessEvent(Long actionId) {
            super(SUCCESS, actionId);
        }
    }

    public static class UpdateConfigRequestEvent extends Event{

        private static final long serialVersionUID = -3653436535402906385L;

        public UpdateConfigRequestEvent() {
            super(UPDATE_CONFIG_REQUEST);
        }

    }

    public static class UpdateFoundEvent extends EventWithActionId{

        private static final long serialVersionUID = 7650031234900094550L;

        public UpdateFoundEvent(Long actionId) {
            super(UPDATE_FOUND, actionId);
        }
    }

    public static class CancelEvent extends EventWithActionId{

        private static final long serialVersionUID = 4752976669552073063L;

        public CancelEvent(Long actionId) {
            super(CANCEL, actionId);
        }
    }

    public static class DownloadRequestEvent extends Event{

        private static final long serialVersionUID = -2032503661097142398L;

        private final DdiDeploymentBase ddiDeploymentBase;

        public DownloadRequestEvent(DdiDeploymentBase ddiDeploymentBase) {
            super(DOWNLOAD_REQUEST);
            this.ddiDeploymentBase = ddiDeploymentBase;
        }

        public DdiDeploymentBase getDdiDeploymentBase() {
            return ddiDeploymentBase;
        }
    }

    public static class FileDownloadedEvent extends Event{

        private static final long serialVersionUID = 7577600239523527986L;
        private final InputStream inputStream;
        private final String fileName;

        public FileDownloadedEvent(InputStream inputStream,
                                   String fileName) {
            super(FILE_DOWNLOADED);
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

    public static class FileCorruptedEvent extends Event{

        private static final long serialVersionUID = 7316816218729999447L;

        private final Hash downloadedFileHash;

        public FileCorruptedEvent(Hash downloadedFileHash) {
            super(FILE_CORRUPTED);
            this.downloadedFileHash = downloadedFileHash;
        }

        public Hash getDownloadedFileHash() {
            return downloadedFileHash;
        }
    }

    public static class ErrorEvent extends Event{

        private static final long serialVersionUID = -5100718411655042146L;
        private final String[] details;
        private final int code;

        public ErrorEvent(String[] details, int code) {
            super(ERROR);
            this.details = details;
            this.code = code;
        }

        public String[] getDetails() {
            return details;
        }

        public int getCode() {
            return code;
        }
    }

    public static class UpdateErrorEvent extends Event{

        private static final long serialVersionUID = -1971119330952172189L;
        private final String[] details;

        public UpdateErrorEvent(String[] details) {
            super(UPDATE_ERROR);
            this.details = details;
        }

        public String[] getDetails() {
            return details;
        }
    }

    public static class AuthorizationGrantedEvent extends Event{

        private static final long serialVersionUID = 5441570052912573129L;

        public AuthorizationGrantedEvent() {
            super(AUTHORIZATION_GRANTED);
        }
    }

    public static class AuthorizationDeniedEvent extends Event{

        private static final long serialVersionUID = 5448969185950732296L;

        public AuthorizationDeniedEvent() {
            super(AUTHORIZATION_DENIED);
        }
    }

    public static class ResumeEvent extends Event{

        private static final long serialVersionUID = -279400326463388492L;

        public ResumeEvent() {
            super(RESUME);
        }
    }
}
