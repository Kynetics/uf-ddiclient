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

import com.kynetics.updatefactory.ddiclient.api.model.response.DdiArtifact;
import com.kynetics.updatefactory.ddiclient.api.model.response.DdiChunk;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.kynetics.updatefactory.ddiclient.api.model.response.DdiDeployment.HandlingType.FORCED;
import static com.kynetics.updatefactory.ddiclient.core.model.State.StateName.*;

/**
 * @author Daniele Sergio
 */
public abstract class State implements Serializable{
    private static final long serialVersionUID = -5320330427452976158L;

    public enum StateName{
        WAITING, CONFIG_DATA, UPDATE_INITIALIZATION, UPDATE_DOWNLOAD, SAVING_FILE, UPDATE_READY, UPDATE_STARTED, CANCELLATION_CHECK,
        CANCELLATION, UPDATE_ENDED, COMMUNICATION_FAILURE, COMMUNICATION_ERROR, AUTHORIZATION_WAITING, SERVER_FILE_CORRUPTED
    }

    private final StateName stateName;
    public State(StateName stateName) {
        this.stateName = stateName;
    }

    public StateName getStateName() {
        return stateName;
    }

    public State onEvent(Event event){
        switch (event.getEventName()){
            case ERROR:
                Event.ErrorEvent errorEvent = (Event.ErrorEvent) event;
                return new CommunicationErrorState(this,errorEvent.getCode(),errorEvent.getDetails());
            case FAILURE:
                Event.FailureEvent failureEvent = (Event.FailureEvent) event;
                return new CommunicationFailureState(this,failureEvent.getThrowable());
            default:
                throw new IllegalStateException(String.format("Event %s not handler in %s state", event.getEventName(), stateName));
        }
    }


    public static class WaitingState extends State{
        private static final long serialVersionUID = 1418151631214100403L;

        private final long sleepTime;

        private final StateWithAction suspendState;

        public WaitingState(long sleepTime, StateWithAction suspendState){
            super(WAITING);
            this.sleepTime = sleepTime;
            this.suspendState = suspendState;
        }
        public long getSleepTime() {
            return sleepTime;
        }

        @Override
        public State onEvent(Event event) {
            switch (event.getEventName()){
                case SLEEP_REQUEST:
                    return new WaitingState(((Event.SleepEvent)event).getSleepTime(), suspendState);
                case UPDATE_CONFIG_REQUEST:
                    return new ConfigDataState();
                case UPDATE_FOUND:
                    final Event.UpdateFoundEvent updateFoundEvent = (Event.UpdateFoundEvent) event;
                    return suspendState != null && updateFoundEvent.getActionId() == suspendState.getActionId() ?
                            this :
                            new UpdateInitialization(((Event.UpdateFoundEvent)event).getActionId());
                case CANCEL:
                    return new CancellationCheckState(this, ((Event.CancelEvent) event).getActionId());
                case RESUME:
                    return new AuthorizationWaitingState(suspendState);
                default:
                    return super.onEvent(event);
            }

        }

        public StateWithAction getSuspendState() {
            return suspendState;
        }

        public boolean hasSuspendState(){
            return suspendState != null;
        }
    }

    public static class ConfigDataState extends State{
        private static final long serialVersionUID = -6379269952019151139L;

        public ConfigDataState() {
            super(CONFIG_DATA);
        }

        @Override
        public State onEvent(Event event) {
            switch (event.getEventName()){
                case SUCCESS:
                    return new WaitingState(0, null);
                default:
                    return super.onEvent(event);
            }
        }
    }

    public static abstract class StateWithAction extends State{
        private static final long serialVersionUID = 3107687832645865189L;

        private final Long actionId;

        public StateWithAction(StateName stateName, Long actionId) {
            super(stateName);
            this.actionId = actionId;
        }

        public Long getActionId() {
            return actionId;
        }
    }

    public static abstract class AbstractUpdateState extends StateWithAction{
        private static final long serialVersionUID = 6475503872241178057L;

        private final boolean isForced;

        public AbstractUpdateState(StateName stateName, Long actionId, boolean isForced) {
            super(stateName, actionId);
            this.isForced = isForced;
        }

        public boolean isForced() {
            return isForced;
        }
    }

    public static class UpdateInitialization extends StateWithAction{

        private static final long serialVersionUID = -3633361480103392026L;

        public UpdateInitialization(Long actionId) {
            super(UPDATE_INITIALIZATION, actionId);
        }

        @Override
        public State onEvent(Event event) {
            switch (event.getEventName()) {
                case DOWNLOAD_REQUEST:
                    final Event.DownloadRequestEvent downloadRequestEvent = ((Event.DownloadRequestEvent)event);
                    final List<FileInfo> fileInfoList = new ArrayList<>();
                    for(DdiChunk chunk: downloadRequestEvent.getDdiDeploymentBase().getDeployment().getChunks()){
                        for(DdiArtifact artifact : chunk.getArtifacts()){
                            fileInfoList.add(new FileInfo(
                                    artifact.getLink("download-http").parseLink2(),
                                    new Hash(artifact.getHashes().getMd5(),
                                            artifact.getHashes().getSha1())));
                        }
                    }
                    final boolean isForced = downloadRequestEvent.getDdiDeploymentBase().getDeployment().getDownload() == FORCED;
                    final State state = new UpdateDownloadState(getActionId(), isForced, fileInfoList, 0);
                    return  isForced ? state : new AuthorizationWaitingState(state) ;

                default:
                    return super.onEvent(event);
            }
        }
    }

    public static class AbstractStateWithFile extends AbstractUpdateState{

        private static final long serialVersionUID = -7333406672136323945L;

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

        public int getSize(){
            return fileInfoList.size();
        }

        public int getNextFileToDownload(){
            return nextFileToDownload;
        }

        public List<FileInfo> getFileInfoList() {
            return Collections.unmodifiableList(fileInfoList);
        }

        public Hash getLastHash() {
            return lastHash;
        }
    }

    public static class UpdateDownloadState extends AbstractStateWithFile{
        private static final long serialVersionUID = 8118466462503137691L;


        public UpdateDownloadState(Long actionId,
                                   boolean isForced,
                                   List<FileInfo> fileInfoList,
                                   int nextFileToDownload,
                                   Hash lastHash) {
            super(UPDATE_DOWNLOAD, actionId, isForced, fileInfoList, nextFileToDownload, lastHash);
        }

        public UpdateDownloadState(Long actionId, boolean isForced, List<FileInfo> fileInfoList, int nextFileToDownload) {
            this(actionId,isForced, fileInfoList, nextFileToDownload, null);
        }

        @Override
        public State onEvent(Event event) {
            switch (event.getEventName()){
                case FILE_DOWNLOADED:
                    return new SavingFileState(getActionId(),isForced(),getFileInfoList(),getNextFileToDownload(), getLastHash(), ((Event.FileDownloadedEvent)event).getInputStream());
                default:
                    return super.onEvent(event);
            }
        }
    }

    public static class SavingFileState extends AbstractStateWithFile{
        private static final long serialVersionUID = -4781913678780210095L;

        public SavingFileState(Long actionId, boolean isForced, List<FileInfo> fileInfoList, int nextFileToDownload, Hash lastHash, InputStream inputStream) {
            super(SAVING_FILE, actionId,isForced, fileInfoList, nextFileToDownload, lastHash);
            this.inputStream = inputStream;
        }

        @Override
        public State onEvent(Event event) {
            switch (event.getEventName()){
                case SUCCESS:
                    return getSize() - 1 == getNextFileToDownload() ?
                            new UpdateReadyState(getActionId(), isForced()) :
                            new UpdateDownloadState(getActionId(), isForced(), getFileInfoList(), getNextFileToDownload() +1);
                case FILE_CORRUPTED:
                    final Event.FileCorruptedEvent corruptedEvent = (Event.FileCorruptedEvent) event;
                    final Hash currentHash = corruptedEvent.getDownloadedFileHash();
                    return  currentHash == null || getLastHash() == null || !currentHash.equals(getLastHash()) ?
                            new UpdateDownloadState(getActionId(),
                                    isForced(),
                                    getFileInfoList(),
                                    getNextFileToDownload(),
                                    currentHash) :
                            new ServerFileCorruptedState(getActionId());

                default:
                    return super.onEvent(event);
            }
        }

        public InputStream getInputStream() {
            return inputStream;
        }

        private final InputStream inputStream;
    }


    public static class ServerFileCorruptedState extends StateWithAction{
        public ServerFileCorruptedState(Long actionId) {
            super(SERVER_FILE_CORRUPTED, actionId);
        }

        @Override
        public State onEvent(Event event) {
            switch (event.getEventName()){
                case SUCCESS:
                    return new WaitingState(0,null);
                default:
                    return super.onEvent(event);
            }
        }
    }

    public static class UpdateReadyState extends AbstractUpdateState{
        private static final long serialVersionUID = -6261104686549440294L;

        public UpdateReadyState(Long actionId, boolean isForced) {
            super(UPDATE_READY, actionId, isForced);
        }

        @Override
        public State onEvent(Event event) {
            switch (event.getEventName()){
                case CANCEL:
                    return new CancellationCheckState(this, ((Event.CancelEvent) event).getActionId());
                case SUCCESS:
                    final StateWithAction state = new UpdateStartedState(getActionId());
                    return isForced() ? state : new AuthorizationWaitingState(state);
                default:
                    return super.onEvent(event);
            }
        }
    }

    public static class UpdateStartedState extends StateWithAction{
        private static final long serialVersionUID = 4112225711938631866L;

        public UpdateStartedState(Long actionId) {
            super(UPDATE_STARTED, actionId);
        }

        @Override
        public State onEvent(Event event) {
            switch (event.getEventName()){
                case SUCCESS:
                    return new UpdateEndedState(getActionId(), true, new String[0]);
                case UPDATE_ERROR:
                    final Event.UpdateErrorEvent errorEvent = (Event.UpdateErrorEvent) event;
                    return new UpdateEndedState(getActionId(), false, errorEvent.getDetails());
                default:
                    return super.onEvent(event);
            }
        }
    }

    public static class UpdateEndedState extends StateWithAction{
        private static final long serialVersionUID = -8423028324228442845L;

        private final boolean isSuccessfullyUpdate;
        private final String[] details;

        public UpdateEndedState(Long actionId, boolean isSuccessfullyUpdate, String[] details) {
            super(UPDATE_ENDED, actionId);
            this.isSuccessfullyUpdate = isSuccessfullyUpdate;
            this.details = details;
        }

        @Override
        public State onEvent(Event event) {
            switch (event.getEventName()){
                case SUCCESS:
                    return new WaitingState(0,null);
                default:
                    return super.onEvent(event);
            }
        }

        public boolean isSuccessfullyUpdate() {
            return isSuccessfullyUpdate;
        }

        public String[] getDetails() {
            return details;
        }
    }

    public static class CancellationCheckState extends StateWithAction{
        private static final long serialVersionUID = -2556360283062075926L;

        private final State previousState;

        public CancellationCheckState(State previousState, Long actionId) {
            super(CANCELLATION_CHECK, actionId);
            this.previousState = previousState;
        }

        @Override
        public State onEvent(Event event) {
            switch (event.getEventName()){
                case SUCCESS: //must cancel the action into the event (successEvent.getActionId()) but I need to send the feedback to the action inside the nextFileToDownload state (getAction);
                    if(getPreviousState().getStateName() == UPDATE_READY){
                        final Event.SuccessEvent successEvent = (Event.SuccessEvent) event;
                        final UpdateReadyState updateReadyState = (UpdateReadyState) getPreviousState();
                        return updateReadyState.getActionId() == successEvent.getActionId() ? new CancellationState(getActionId()) : new UpdateStartedState(updateReadyState.getActionId());
                    }
                    return new CancellationState(getActionId());
                default:
                    return super.onEvent(event);
            }
        }

        public State getPreviousState() {
            return previousState;
        }
    }

    public static class CancellationState extends StateWithAction{
        private static final long serialVersionUID = -8128478062633603709L;

        public CancellationState(Long actionId) {
            super(CANCELLATION, actionId);
        }

        @Override
        public State onEvent(Event event) {
            switch (event.getEventName()){
                case SUCCESS:
                    return new WaitingState(0, null);
                default:
                    return super.onEvent(event);
            }
        }
    }

    public abstract static class AbstractStateWithInnerState extends State{
        private static final long serialVersionUID = 68418421182598220L;

        private final State state;

        public AbstractStateWithInnerState(StateName stateName, State state) {
            super(stateName);
            this.state = state;
        }

        public State getState() {
            return state;
        }

    }

    public static class CommunicationFailureState extends AbstractStateWithInnerState {

        private static final long serialVersionUID = 2252291454139960637L;

        private final Throwable throwable;

        public CommunicationFailureState(State state, Throwable throwable) {
            super(COMMUNICATION_FAILURE, state);
            this.throwable = throwable;
        }

        public Throwable getThrowable() {
            return throwable;
        }

        @Override
        public State onEvent(Event event) {
            switch (event.getEventName()){
                case FAILURE:
                    return this;
                case ERROR:
                    Event.ErrorEvent errorEvent = (Event.ErrorEvent) event;
                    return new CommunicationErrorState(this, errorEvent.getCode(),errorEvent.getDetails());
                default:
                    return getState().onEvent(event);
            }
        }
    }

    public static class CommunicationErrorState extends AbstractStateWithInnerState {
        private static final long serialVersionUID = -7238517877916633281L;

        private final int code;
        private final String[] details;
        public CommunicationErrorState(State state, int code, String[] details) {
            super(COMMUNICATION_ERROR, state);
            this.code = code;
            this.details = details;
        }

        public long getCode() {
            return code;
        }

        public String[] getDetails() {
            return details.clone();
        }

        @Override
        public State onEvent(Event event) {
            switch (event.getEventName()){
                case ERROR:
                    return this;
                case FAILURE:
                    Event.FailureEvent errorEvent = (Event.FailureEvent) event;
                    return new CommunicationFailureState(this, errorEvent.getThrowable());
                default:
                    return getState().onEvent(event);
            }
        }
    }

    public static class AuthorizationWaitingState extends AbstractStateWithInnerState{

        private static final long serialVersionUID = 8842122473954599874L;

        public AuthorizationWaitingState(State state) {
            super(AUTHORIZATION_WAITING, state);
        }

        @Override
        public State onEvent(Event event) {
            switch (event.getEventName()){
                case AUTHORIZATION_GRANTED:
                    return  getState();
                case AUTHORIZATION_DENIED:
                    return  new WaitingState(30_000, (StateWithAction) getState()); // FIXME: 9/11/17 sleeptime should be the last sleeptime found
                default:
                    return super.onEvent(event);
            }
        }
    }

}
