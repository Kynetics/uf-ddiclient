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
import static com.kynetics.updatefactory.ddiclient.core.model.State.AbstractCommunicationState.MAX_ATTEMPTS;
import static com.kynetics.updatefactory.ddiclient.core.model.State.StateName.*;

/**
 * @author Daniele Sergio
 */
public abstract class State implements Serializable{
    private static final long serialVersionUID = 8624104209862658206L;

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
                return getStateOnError(errorEvent, this, MAX_ATTEMPTS);
            case FAILURE:
                Event.FailureEvent failureEvent = (Event.FailureEvent) event;
                return new CommunicationFailureState(this,failureEvent.getThrowable());
            default:
                throw new IllegalStateException(String.format("Event %s not handler in %s state", event.getEventName(), stateName));
        }
    }

    private static State getStateOnError(Event.ErrorEvent errorEvent, State state, int retry) {
        return errorEvent.getCode() == 404 && errorEvent.getDetails()[0] != null &&
                errorEvent.getDetails()[0].equals("hawkbit.server.error.repo.entitiyNotFound") ?
                new WaitingState(0,null) :
                new CommunicationErrorState(state,retry, errorEvent.getCode(), errorEvent.getDetails());
    }


    public static class WaitingState extends AbstractStateWithInnerState{
        private static final long serialVersionUID = -8905024383731749954L;

        private final long sleepTime;

        public WaitingState(long sleepTime, State suspendState){
            super(WAITING, suspendState);
            this.sleepTime = sleepTime;
        }
        public long getSleepTime() {
            return sleepTime;
        }

        @Override
        public State onEvent(Event event) {
            switch (event.getEventName()){
                case SLEEP_REQUEST:
                    return new WaitingState(((Event.SleepEvent)event).getSleepTime(), getState());
                case UPDATE_CONFIG_REQUEST:
                    return new ConfigDataState();
                case UPDATE_FOUND:
                    final Event.UpdateFoundEvent updateFoundEvent = (Event.UpdateFoundEvent) event;
                    return hasInnerState() && updateFoundEvent.getActionId().equals(getInnerStateActionId()) ?
                            this :
                            new UpdateInitialization(((Event.UpdateFoundEvent)event).getActionId());
                case CANCEL:
                    return new CancellationCheckState(this, ((Event.CancelEvent) event).getActionId());
                case RESUME:
                    return innerStateIsCommunicationState() ?
                            getMostInnerState() :
                            new AuthorizationWaitingState(getState());
                default:
                    return super.onEvent(event);
            }

        }

        public boolean innerStateIsCommunicationState(){
            if(!hasInnerState()){
                return false;
            }
            final StateName innerStateName = getState().getStateName();
            return innerStateName == COMMUNICATION_ERROR || innerStateName == COMMUNICATION_FAILURE;
        }

        private State getMostInnerState(){
            return innerStateIsCommunicationState() ? ((AbstractCommunicationState)getState()).getState() : getState();
        }

        private Long getInnerStateActionId(){
            final State state = getMostInnerState();
            return  state instanceof StateWithAction ?
                    ((StateWithAction)state).getActionId() :
                    null;
        }
    }

    public static class ConfigDataState extends State{
        private static final long serialVersionUID = 1510486692091734525L;

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
        private static final long serialVersionUID = -6011000748604690610L;

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
        private static final long serialVersionUID = -2826703270286073179L;

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

        private static final long serialVersionUID = -703356064985341858L;

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
                    final boolean noFile = fileInfoList.size() == 0;
                    final State state = noFile ?
                            new UpdateEndedState(getActionId(), true, new String[]{"Update doesn't have file"}) :
                            new UpdateDownloadState(getActionId(), isForced, fileInfoList, 0);
                    return  isForced || noFile ? state : new AuthorizationWaitingState(state) ;

                default:
                    return super.onEvent(event);
            }
        }
    }

    public abstract static class AbstractStateWithFile extends AbstractUpdateState{

        private static final long serialVersionUID = -6785284114160973170L;

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
        private static final long serialVersionUID = -1998879559588928971L;


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
        private static final long serialVersionUID = -4330529885840336590L;

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

        transient private final InputStream inputStream;
    }


    public static class ServerFileCorruptedState extends StateWithAction{

        private static final long serialVersionUID = 3171662012367375837L;

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
        private static final long serialVersionUID = -8501350119987754124L;

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
        private static final long serialVersionUID = 6238930315498186356L;

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
        private static final long serialVersionUID = -868250366352211123L;

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
        private static final long serialVersionUID = -771773165049497602L;

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
                        return updateReadyState.getActionId() == successEvent.getActionId() ?
                                new CancellationState(getActionId()) :
                                updateReadyState.isForced() ?
                                        new AuthorizationWaitingState(updateReadyState) :
                                        new UpdateStartedState(updateReadyState.getActionId());
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
        private static final long serialVersionUID = -2805589128447579587L;

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
        private static final long serialVersionUID = 8985897172839137083L;

        private final State state;

        public AbstractStateWithInnerState(StateName stateName, State state) {
            super(stateName);
            this.state = state;
        }

        public State getState() {
            return state;
        }

        public boolean hasInnerState(){
            return state != null;
        }

    }

    public static abstract class AbstractCommunicationState extends AbstractStateWithInnerState{

        static final int MAX_ATTEMPTS = 5;

        private static final long serialVersionUID = -2987878585212623357L;

        private final int attemptsRemaining;

        public AbstractCommunicationState(StateName stateName, State state, int attemptsRemaining) {
            super(stateName, state);
            if(state != null && state.getStateName().equals(UPDATE_DOWNLOAD)){
                this.attemptsRemaining = attemptsRemaining;
            } else {
                this.attemptsRemaining = MAX_ATTEMPTS;
            }
        }

        @Override
        public State onEvent(Event event) {
            switch (event.getEventName()){
                case ERROR:
                    final Event.ErrorEvent errorEvent = (Event.ErrorEvent)event;
                    return attemptsRemaining == 0 ? new WaitingState(0, this) : getStateOnError(errorEvent, getState(), attemptsRemaining -1);
                case FAILURE:
                    Event.FailureEvent failureEvent = (Event.FailureEvent) event;
                    return attemptsRemaining == 0 ? new WaitingState(0, this) : new CommunicationFailureState(getState(), attemptsRemaining -1, failureEvent.getThrowable() );
                default:
                    return getState().onEvent(event);
            }
        }
    }

    public static class CommunicationFailureState extends AbstractCommunicationState {

        private static final long serialVersionUID = -2674723538926673012L;

        private final Throwable throwable;

        public CommunicationFailureState( State state, int retry, Throwable throwable) {
            super(COMMUNICATION_FAILURE, state, retry);
            this.throwable = throwable;
        }

        public CommunicationFailureState(State state, Throwable throwable) {
            this(state, MAX_ATTEMPTS, throwable);
        }

        public Throwable getThrowable() {
            return throwable;
        }

    }

    public static class CommunicationErrorState extends AbstractCommunicationState {
        private static final long serialVersionUID = 3416210232557335517L;

        private final int code;
        private final String[] details;

        public CommunicationErrorState(State state, int retry, int code, String[] details) {
            super(COMMUNICATION_ERROR, state, retry);
            this.code = code;
            this.details = details;
        }

        public CommunicationErrorState(State state, int code, String[] details) {
            this(state, MAX_ATTEMPTS, code, details);
        }

        public long getCode() {
            return code;
        }

        public String[] getDetails() {
            return details.clone();
        }
    }

    public static class AuthorizationWaitingState extends AbstractStateWithInnerState{

        private static final long serialVersionUID = -5937351953114830670L;

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
