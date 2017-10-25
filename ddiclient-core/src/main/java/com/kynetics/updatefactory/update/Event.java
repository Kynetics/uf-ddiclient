/*
 * Copyright © 2017 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.updatefactory.update;

import com.kynetics.updatefactory.ddiclient.api.model.response.DdiDeploymentBase;

import java.io.InputStream;
import java.io.Serializable;

import static com.kynetics.updatefactory.update.Event.EventName.*;

/**
 * @author Daniele Sergio
 */
public class Event implements Serializable{

    private static final long serialVersionUID = -4282270154808438827L;

    public enum EventName{
        SLEEP_REQUEST,  UPDATE_CONFIG_REQUEST, SUCCESS, FAILURE, ERROR, UPDATE_FOUND, DOWNLOAD_REQUEST, FILE_DOWNLOADED,
        CANCEL, UPDATE_ERROR, AUTHORIZATION_GRANTED, AUTHORIZATION_DENIED, RESUME
    }

    private final EventName eventName;

    public Event(EventName eventName) {
        this.eventName = eventName;
    }

    public EventName getEventName() {
        return eventName;
    }

    public static class SleepEvent extends Event{

        private static final long serialVersionUID = -1814494385021105781L;

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

        private static final long serialVersionUID = -792237867960643822L;

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

        private static final long serialVersionUID = 4571448143777394644L;

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

        private static final long serialVersionUID = 7477070573336483724L;

        public SuccessEvent() {
            super(SUCCESS,null);
        }

        public SuccessEvent(Long actionId) {
            super(SUCCESS, actionId);
        }
    }

    public static class UpdateConfigRequestEvent extends Event{

        private static final long serialVersionUID = -8056650122962354394L;

        public UpdateConfigRequestEvent() {
            super(UPDATE_CONFIG_REQUEST);
        }

    }

    public static class UpdateFoundEvent extends EventWithActionId{

        private static final long serialVersionUID = 4985927319066489586L;

        public UpdateFoundEvent(Long actionId) {
            super(UPDATE_FOUND, actionId);
        }
    }

    public static class CancelEvent extends EventWithActionId{

        private static final long serialVersionUID = 703860981008558989L;

        public CancelEvent(Long actionId) {
            super(CANCEL, actionId);
        }
    }

    public static class DownloadRequestEvent extends Event{

        private static final long serialVersionUID = -7005721027575721248L;

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

        private static final long serialVersionUID = 482227239739618846L;
        private final InputStream inputStream;
        private final String fileName;
        private final String shae1;
        private final String md5;

        public FileDownloadedEvent(InputStream inputStream,
                                   String fileName,
                                   String shae1,
                                   String md5) {
            super(FILE_DOWNLOADED);
            this.inputStream = inputStream;
            this.fileName = fileName;
            this.md5 = md5;
            this.shae1 = shae1;
        }

        public InputStream getInputStream() {
            return inputStream;
        }

        public String getFileName() {
            return fileName;
        }

        public String getShae1() {
            return shae1;
        }

        public String getMd5() {
            return md5;
        }
    }
    public static class ErrorEvent extends Event{

        private static final long serialVersionUID = 8090013159293139724L;
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

        private static final long serialVersionUID = 1501593804339427112L;
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


        private static final long serialVersionUID = 8476644426568516165L;

        public AuthorizationGrantedEvent() {
            super(AUTHORIZATION_GRANTED);
        }
    }

    public static class AuthorizationDeniedEvent extends Event{

        private static final long serialVersionUID = 8190466160344336819L;

        public AuthorizationDeniedEvent() {
            super(AUTHORIZATION_DENIED);
        }
    }

    public static class ResumeEvent extends Event{

        private static final long serialVersionUID = 8081456162156585815L;

        public ResumeEvent() {
            super(RESUME);
        }
    }
}
