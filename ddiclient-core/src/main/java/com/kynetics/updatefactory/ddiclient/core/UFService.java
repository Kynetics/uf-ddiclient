/*
 * Copyright © 2017-2018 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.updatefactory.ddiclient.core;

import com.google.gson.Gson;
import com.kynetics.updatefactory.ddiclient.core.filterInputStream.CheckFilterInputStream;
import com.kynetics.updatefactory.ddiclient.core.formatter.CurrentTimeFormatter;
import com.kynetics.updatefactory.ddiclient.core.model.*;
import com.kynetics.updatefactory.ddiclient.api.DdiCallback;
import com.kynetics.updatefactory.ddiclient.api.api.DdiRestApi;
import com.kynetics.updatefactory.ddiclient.api.api.DdiRestConstants;
import com.kynetics.updatefactory.ddiclient.api.model.request.*;
import com.kynetics.updatefactory.ddiclient.api.model.request.DdiResult.FinalResult;
import com.kynetics.updatefactory.ddiclient.api.model.request.DdiStatus.ExecutionStatus;
import com.kynetics.updatefactory.ddiclient.api.model.response.DdiCancel;
import com.kynetics.updatefactory.ddiclient.api.model.response.DdiControllerBase;
import com.kynetics.updatefactory.ddiclient.api.model.response.DdiDeploymentBase;
import com.kynetics.updatefactory.ddiclient.api.model.response.Error;
import com.kynetics.updatefactory.ddiclient.api.model.response.ResourceSupport.LinkEntry;
import com.kynetics.updatefactory.ddiclient.core.model.event.*;
import com.kynetics.updatefactory.ddiclient.core.model.state.*;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.kynetics.updatefactory.ddiclient.api.model.request.DdiResult.FinalResult.*;
import static com.kynetics.updatefactory.ddiclient.api.model.request.DdiStatus.ExecutionStatus.*;

/**
 * @author Daniele Sergio
 */
public class  UFService {

    public interface TargetData {
        Map<String, String> get();
    }

    public static class SharedEvent {
        private final AbstractEvent event;
        private final AbstractState newState;
        private final AbstractState oldState;

        public SharedEvent(AbstractEvent event, AbstractState newState, AbstractState oldState) {
            this.event = event;
            this.newState = newState;
            this.oldState = oldState;
        }

        public AbstractEvent getEvent() {
            return event;
        }

        public AbstractState getNewState() {
            return newState;
        }

        public AbstractState getOldState() {
            return oldState;
        }
    }

    public static UFServiceBuilder builder(){
        return new UFServiceBuilder();
    }

    UFService(DdiRestApi client,
              String tenant,
              String controllerId,
              AbstractState initialState,
              TargetData targetData,
              long retryDelayOnCommunicationError){
        currentObservableState = new ObservableState(initialState);
        this.client = client;
        this.retryDelayOnCommunicationError = retryDelayOnCommunicationError;
        this.tenant = tenant;
        this.controllerId = controllerId;
        this.targetData = targetData;
    }

    public void start(){
        if(timer!=null){
            stop();
        }
        timer = new Timer();
        final AbstractState state = currentObservableState.get();
        handlerState(state);
        if(state.getStateName() != AbstractState.StateName.WAITING) {
            return;
        }

        final WaitingState waitingState = (WaitingState) state;

        if(waitingState.innerStateIsCommunicationState()){
            restartSuspendState();
        }

    }

    public void stop(){
        if(timer == null){
            return;
        }
        timer.cancel();
        timer.purge();
        timer = null;
        currentObservableState.resetState();
    }

    public void restart(){
        stop();
        start();
    }

    public void addObserver(Observer observer){
        currentObservableState.addObserver(observer);
    }

    public void setUpdateSucceffullyUpdate(boolean success){
        checkServiceRunning();
        if(!currentObservableState.get().getStateName().equals(AbstractState.StateName.UPDATE_STARTED)){
            throw new IllegalStateException("current state must be UPDATE_STARTED to call this method");
        }
        onEvent(success ? new SuccessEvent() : new UpdateErrorEvent(new String[]{"update error"}));
    }

    private void checkServiceRunning(){
        if(timer == null){
            throw  new IllegalStateException("service isn't yet started");
        }
    }

    public void setAuthorized(boolean isAuthorized){
        checkServiceRunning();
        if(!currentObservableState.get().getStateName().equals(AbstractState.StateName.AUTHORIZATION_WAITING)){
            throw new IllegalStateException("current state must be UPDATE_STARTED to call this method");
        }
        onEvent(isAuthorized ? new AuthorizationGrantedEvent() : new AuthorizationDeniedEvent());
    }

    public void restartSuspendState(){
        checkServiceRunning();
        if(!currentObservableState.get().getStateName().equals(AbstractState.StateName.WAITING)){
            throw new IllegalStateException("current state must be WAITING to call this method");
        }
        timer.cancel();
        timer.purge();
        timer = new Timer();
        onEvent(new ResumeEvent());
    }

    private void execute(Call call, Callback callback, long delay){
        timer.schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        call.enqueue(callback);
                    }
                },
                delay
        );
    }

    private void execute(Call call, Callback callback){
        execute(call,callback,0);
    }

    private void onEvent(AbstractEvent event){
        final AbstractState currentState = currentObservableState.onEvent(event).get();
        handlerState(currentState);
    }

    private void handlerState(AbstractState currentState, long forceDelay){
        switch (currentState.getStateName()){
            case WAITING:
                final Call controllerBaseCall =  client.getControllerBase(tenant, controllerId);
                final WaitingState waitingState = ((WaitingState)currentState);
                forceDelay = waitingState.innerStateIsCommunicationState() ? LAST_SLEEP_TIME_FOUND : forceDelay;
                execute(controllerBaseCall, new PollingCallback(), forceDelay > 0 ? forceDelay : waitingState.getSleepTime());
                break;
            case CONFIG_DATA:
                final Call call = getConfigDataCall();
                execute(call, new DefaultDdiCallback(), forceDelay);
                break;
            case UPDATE_INITIALIZATION:
                final UpdateInitialization updateInitializationState = (UpdateInitialization) currentState;
                final Call baseDeploymentAction = client.getControllerBasedeploymentAction(
                        tenant,
                        controllerId,
                        updateInitializationState.getActionId(),
                        DdiRestConstants.DEFAULT_RESOURCE,
                        DdiRestConstants.NO_ACTION_HISTORY);
                client.postBasedeploymentActionFeedback(
                        new FeedbackBuilder(updateInitializationState.getActionId(),SCHEDULED,NONE).build(),
                        tenant,
                        controllerId,
                        updateInitializationState.getActionId()).enqueue(new LogCallBack<>());
                execute(baseDeploymentAction, new ControllerBaseDeploymentAction(), forceDelay);
                break;
            case UPDATE_DOWNLOAD:
                final UpdateDownloadState updateDownloadState = (UpdateDownloadState) currentState;
                final Call downloadArtifactCall = client.downloadArtifact(
                        tenant,
                        controllerId,
                        updateDownloadState.getFileInfo().getLinkInfo().getSoftwareModules(),
                        updateDownloadState.getFileInfo().getLinkInfo().getFileName());
                execute(downloadArtifactCall, new DownloadArtifact(updateDownloadState), forceDelay);
                break;
            case UPDATE_READY:
                final Call checkCancellation = client.getControllerBase(tenant, controllerId);
                execute(checkCancellation,new CheckCancellationCallback(), forceDelay);
                break;
            case CANCELLATION_CHECK:
                final CancellationCheckState cancellationCheckState = (CancellationCheckState)currentState;
                final Call controllerCancelAction = client.getControllerCancelAction(tenant,controllerId,cancellationCheckState.getActionId());
                execute(controllerCancelAction, new CancelCallback(), forceDelay);
                break;
            case CANCELLATION:
                final CancellationState cancellationState = (CancellationState)currentState;
                DdiActionFeedback actionFeedback = new FeedbackBuilder(cancellationState.getActionId(),CLOSED, SUCESS).build();
                final Call postCancelActionFeedback = client.postCancelActionFeedback(actionFeedback, tenant,controllerId,cancellationState.getActionId());
                execute(postCancelActionFeedback, new DefaultDdiCallback(), forceDelay);
                break;
            case UPDATE_STARTED:
            case AUTHORIZATION_WAITING:
                break;
            case UPDATE_ENDED:
                final UpdateEndedState updateEndedState = (UpdateEndedState)currentState;

                if(updateEndedState.isSuccessfullyUpdate()){
                    getConfigDataCall().enqueue(new LogCallBack());
                }

                final DdiActionFeedback lastFeedback = new FeedbackBuilder(updateEndedState.getActionId(),CLOSED,
                        updateEndedState.isSuccessfullyUpdate() ? SUCESS : FAILURE)
                        .withDetails(Arrays.asList(updateEndedState.getDetails()))
                        .build();

                final Call lastFeedbackCall = client.postBasedeploymentActionFeedback(
                        lastFeedback,
                        tenant,
                        controllerId,
                        updateEndedState.getActionId()
                );
                execute(lastFeedbackCall,new DefaultDdiCallback(), forceDelay);
                break;
            case SERVER_FILE_CORRUPTED:
                final ServerFileCorruptedState serverFileCorruptedState = (ServerFileCorruptedState)currentState;

                final DdiActionFeedback serverErrorFeedback = new FeedbackBuilder(serverFileCorruptedState.getActionId(),CLOSED,
                        FAILURE)
                        .withDetails(Arrays.asList("SERVER FILE CORRUPTED"))
                        .build();

                final Call serverErrorFeedbackCall = client.postBasedeploymentActionFeedback(
                        serverErrorFeedback,
                        tenant,
                        controllerId,
                        serverFileCorruptedState.getActionId()
                );

                execute(serverErrorFeedbackCall,new DefaultDdiCallback(), forceDelay);
                break;
            case COMMUNICATION_ERROR:
            case COMMUNICATION_FAILURE:
                handlerState(((AbstractStateWithInnerState)currentState).getState(), retryDelayOnCommunicationError);
                break;
        }
        currentObservableState.notifyEvent();
    }

    private Call getConfigDataCall() {
        final CurrentTimeFormatter currentTimeFormatter = new CurrentTimeFormatter();
        final DdiConfigData configData = new DdiConfigData(
                null,
                currentTimeFormatter.formatCurrentTime(),
                new DdiStatus(
                        ExecutionStatus.CLOSED,
                        new DdiResult(
                                FinalResult.SUCESS,
                                null),
                        new ArrayList<>()),
                targetData.get());
        return client.putConfigData(configData,tenant, controllerId);
    }

    private void handlerState(AbstractState currentState) {
        handlerState(currentState,0);
    }

    private static class FeedbackBuilder{
        private List<String> details;
        private DdiProgress progress;
        private ExecutionStatus executionStatus;
        private FinalResult finalResult;
        private Long id;

        private FeedbackBuilder(Long id, ExecutionStatus executionStatus, FinalResult finalResult){
            this.executionStatus = executionStatus;
            this.finalResult = finalResult;
            this.id = id;
        }
        private FeedbackBuilder withDetails(List<String> details){
            this.details = details;
            return this;
        }


        private FeedbackBuilder withProgess(int cnt, int of){
            this.progress = new DdiProgress(cnt,of);
            return this;
        }

        DdiActionFeedback build(){
            final DdiResult result = new DdiResult(finalResult, progress);
            final DdiStatus status = new DdiStatus(executionStatus, result, details);
            final CurrentTimeFormatter currentTimeFormatter = new CurrentTimeFormatter();
            return new DdiActionFeedback(id, currentTimeFormatter.formatCurrentTime(),status);
        }

    }

    private class LogCallBack<T> extends DdiCallback<T>{
        @Override
        public void onError(Error error) {
            System.out.println("onError:   "+ new Gson().toJson(error));
        }

        @Override
        public void onSuccess(T response) {
        }

        @Override
        public void onFailure(Call<T> call, Throwable t) {
            System.out.println("onFailure");
            t.printStackTrace();
        }
    }

    private class ObservableState extends Observable{

        public ObservableState(AbstractState state) {
            this.state = state;
        }

        private AbstractState state;

        private SharedEvent eventToNotify;

        private ObservableState onEvent(AbstractEvent event){
            final AbstractState oldState = state;
            state = state.onEvent(event);
            setChanged();
            eventToNotify = new SharedEvent(event, state, oldState);
            return this;

        }

        private void notifyEvent(){
            if(eventToNotify == null){
                return;
            }
            notifyObservers(eventToNotify);
        }

        private AbstractState get(){return state;}

        private void resetState(){
            state = new WaitingState(0, null);
        }

    }

    private class DefaultDdiCallback<T> extends LogCallBack<T> {
        @Override
        public void onError(Error error) {
            super.onError(error);
            onEvent(new ErrorEvent(new String[]{error.getErrorCode(), error.getMessage()},error.getCode()));
        }

        @Override
        public void onSuccess(T response) {
            super.onSuccess(response);
            onEvent(new SuccessEvent());
        }

        @Override
        public void onFailure(Call<T> call, Throwable t) {
            super.onFailure(call,t);
            onEvent(new FailureEvent(t));
        }
    }

    // TODO: 12/20/17 moved in to PollingCallback
    public static long LAST_SLEEP_TIME_FOUND = 30_000;

    private class PollingCallback extends DefaultDdiCallback<DdiControllerBase>{

        @Override
        public void onSuccess(DdiControllerBase response) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm:ss");
            try {
                simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = simpleDateFormat.parse(response.getConfig().getPolling().getSleep());
                LAST_SLEEP_TIME_FOUND = date.getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }

            final LinkEntry configDataLink = response.getLink("configData");
            if(configDataLink!=null){
                onEvent(new UpdateConfigRequestEvent());
                return;
            }

            final LinkEntry deploymentBaseLink = response.getLink("deploymentBase");

            if(deploymentBaseLink!=null){
                onEvent(new UpdateFoundEvent(deploymentBaseLink.parseLink().getActionId()));
                return;
            }

            final LinkEntry cancelAction = response.getLink("cancelAction");
            if(cancelAction!=null){
                onEvent(new CancelEvent(cancelAction.parseLink().getActionId()));
                return;
            }


            onEvent(new SleepEvent(LAST_SLEEP_TIME_FOUND));
        }
    }

    private class CheckCancellationCallback extends DefaultDdiCallback<DdiControllerBase>{
        @Override
        public void onSuccess(DdiControllerBase response) {
            final LinkEntry cancelAction = response.getLink("cancelAction");
            if(cancelAction!=null){
                onEvent(new CancelEvent(cancelAction.parseLink().getActionId()));
                return;
            }
            onEvent(new SuccessEvent());
        }
    }

    private class CancelCallback extends DefaultDdiCallback<DdiCancel>{
        @Override
        public void onSuccess(DdiCancel response) {
            onEvent(new SuccessEvent(Long.parseLong(response.getCancelAction().getStopId())));
        }
    }

    private class ControllerBaseDeploymentAction extends DefaultDdiCallback<DdiDeploymentBase>{
        @Override
        public void onSuccess(DdiDeploymentBase response) {
            //super.onSuccess(response);
            onEvent(new DownloadRequestEvent(response));
        }
    }

    private class DownloadArtifact extends DefaultDdiCallback<ResponseBody>{
        private final UpdateDownloadState state;

        public DownloadArtifact(UpdateDownloadState state) {
            this.state = state;
        }

        @Override
        public void onSuccess(ResponseBody response) {
            //super.onSuccess(response);
            //onEvent(new AbstractEvent.DownloadRequestEvent(response));
            client.postBasedeploymentActionFeedback(
                    new FeedbackBuilder(state.getActionId(),PROCEEDING,NONE)
                            .withProgess(state.getNextFileToDownload()+1,state.getSize()).build(),
                    tenant,
                    controllerId,
                    state.getActionId())
                    .enqueue(new LogCallBack<>());
            final FileInfo fileInfo = state.getFileInfo();

            final CheckFilterInputStream stream = CheckFilterInputStream.builder()
                    .withStream(response.byteStream())
                    .withMd5Value(fileInfo.getHash().getMd5())
                    .withSha1Value(fileInfo.getHash().getSha1())
                    .withListener(fileCheckListener)
                    .build();

            onEvent(new FileDownloadedEvent(stream,
                    fileInfo.getLinkInfo().getFileName()));
        }
    }


    private final CheckFilterInputStream.FileCheckListener fileCheckListener = (isValid, hash) -> {
        if(isValid){
            onEvent(new SuccessEvent());
        } else {
            onEvent(new FileCorruptedEvent(hash));
        }
    };

    private ObservableState currentObservableState;
    private TargetData targetData;
    private String controllerId;
    private String tenant;
    private Timer timer;
    private DdiRestApi client;
    private long retryDelayOnCommunicationError;
}
