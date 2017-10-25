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

import com.google.gson.Gson;
import com.kynetics.updatefactory.ddiclient.api.ClientBuilder;
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

    public static class SharedEvent {
        private final Event event;
        private final State newState;
        private final State oldState;

        public SharedEvent(Event event, State newState, State oldState) {
            this.event = event;
            this.newState = newState;
            this.oldState = oldState;
        }

        public Event getEvent() {
            return event;
        }

        public State getNewState() {
            return newState;
        }

        public State getOldState() {
            return oldState;
        }
    }

    public static Builder builder(){
        return new Builder();
    }

    public static class Builder {
        private String url;
        private String username;
        private String password;
        private String tenant;
        private String controllerId;
        private State initialState = new State.WaitingState(0, null);
        private long retryDelayOnCommunicationError;

        public Builder withUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder withUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder withPassword(String password) {
            this.password = password;
            return this;
        }

        public Builder withInitialState(State initialState){
            this.initialState = initialState;
            return this;
        }

        public Builder withControllerId(String controllerId){
            this.controllerId = controllerId;
            return this;
        }

        public Builder withTenant(String tenant){
            this.tenant = tenant;
            return this;
        }

        public Builder withRetryDelayOnCommunicationError(long retryDelayOnCommunicationError) {
            this.retryDelayOnCommunicationError = retryDelayOnCommunicationError;
            return this;
        }

        public UFService build() {
            Objects.requireNonNull(url);
            Objects.requireNonNull(username);
            Objects.requireNonNull(password);
            Objects.requireNonNull(initialState);
            Objects.requireNonNull(controllerId);
            Objects.requireNonNull(tenant);
            return new UFService(url, username, password, tenant, controllerId, initialState, retryDelayOnCommunicationError);
        }
    }

    private UFService(String url,
                      String username,
                      String password,
                      String tenant,
                      String controllerId,
                      State initialState,
                      long retryDelayOnCommunicationError){
        currentObservableState = new ObservableState(initialState);
        client = new ClientBuilder()
                .withBaseUrl(url)
                .withPassword(password)
                .withUsername(username)
                .build();
        this.retryDelayOnCommunicationError = retryDelayOnCommunicationError;
        this.tenant = tenant;
        this.controllerId = controllerId;
    }

    public void start(){
        if(timer!=null){
            stop();
        }
        timer = new Timer();
        handlerState(currentObservableState.get());
    }

    public void stop(){
        if(timer == null){
            return;
        }
        timer.cancel();
        timer.purge();
        timer = null;
    }

    public void restart(){
        stop();
        timer = new Timer();
    }

    public void addObserver(Observer observer){
        currentObservableState.addObserver(observer);
    }

    public void setUpdateSucceffullyUpdate(boolean success){
        if(!currentObservableState.get().getStateName().equals(State.StateName.UPDATE_STARTED)){
            throw new IllegalStateException("current state must be UPDATE_STARTED to call this method");
        }
        onEvent(success ? new Event.SuccessEvent() : new Event.UpdateErrorEvent(new String[]{"update error"}));
    }

    public void setAuthorized(boolean isAuthorized){
        if(!currentObservableState.get().getStateName().equals(State.StateName.AUTHORIZATION_WAITING)){
            throw new IllegalStateException("current state must be UPDATE_STARTED to call this method");
        }
        onEvent(isAuthorized ? new Event.AuthorizationGrantedEvent() : new Event.AuthorizationDeniedEvent());
    }

    public void restartSuspendState(){
       if(!currentObservableState.get().getStateName().equals(State.StateName.WAITING)){
           throw new IllegalStateException("current state must be WAITING to call this method");
       }
       restart();
       onEvent(new Event.ResumeEvent());
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

    private void onEvent(Event event){
        final State currentState = currentObservableState.onEvent(event).get();
        handlerState(currentState);
    }

    private void handlerState(State currentState, long forceDelay){
        switch (currentState.getStateName()){
            case WAITING:
                final Call controllerBaseCall =  client.getControllerBase(tenant, controllerId);
                execute(controllerBaseCall, new PollingCallback(), forceDelay > 0 ? forceDelay : ((State.WaitingState)currentState).getSleepTime());
                break;
            case CONFIG_DATA:
                final DdiConfigData configData = getFakeConfigData();
                final Call call =  client.putConfigData(configData,tenant, controllerId);
                execute(call, new DefaultDdiCallback(), forceDelay);
                break;
            case UPDATE_INITIALIZATION:
                final State.UpdateInitialization updateInitializationState = (State.UpdateInitialization) currentState;
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
                final State.UpdateDownloadState updateDownloadState = (State.UpdateDownloadState) currentState;
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
                final State.CancellationCheckState cancellationCheckState = (State.CancellationCheckState)currentState;
                final Call controllerCancelAction = client.getControllerCancelAction(tenant,controllerId,cancellationCheckState.getActionId());
                execute(controllerCancelAction, new CancelCallback(), forceDelay);
                break;
            case CANCELLATION:
                final State.CancellationState cancellationState = (State.CancellationState)currentState;
                DdiActionFeedback actionFeedback = new FeedbackBuilder(cancellationState.getActionId(),CLOSED, SUCESS).build();
                final Call postCancelActionFeedback = client.postCancelActionFeedback(actionFeedback, tenant,controllerId,cancellationState.getActionId());
                execute(postCancelActionFeedback, new DefaultDdiCallback(), forceDelay);
                break;
            case UPDATE_STARTED:
            case AUTHORIZATION_WAITING:
                break;
            case UPDATE_ENDED:
                final State.UpdateEndedState updateEndedState = (State.UpdateEndedState)currentState;
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
            case COMMUNICATION_ERROR:
            case COMMUNICATION_FAILURE:
                handlerState(((State.AbstractStateWithInnerState)currentState).getState(), retryDelayOnCommunicationError);
                break;
        }
        currentObservableState.notifyEvent();
    }

    private void handlerState(State currentState) {
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
            return new DdiActionFeedback(id, getCurrentTimeString(),status);
        }

    }

    private static DdiConfigData getFakeConfigData(){
        Map<String,String> data = new HashMap<>(2);
        data.put("VIN", UUID.randomUUID().toString().replaceAll("-", ""));
        data.put("hwRevision", "2");
        return new DdiConfigData(
                null,
                getCurrentTimeString(),
                new DdiStatus(
                        DdiStatus.ExecutionStatus.CLOSED,
                        new DdiResult(
                                DdiResult.FinalResult.SUCESS,
                                null),
                        new ArrayList<>()),
                data
        );
    }

    //todo refactor -> custom obj
    private static String getCurrentTimeString(){
        TimeZone timeZone = TimeZone.getTimeZone("UTC");
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTime(new Date());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
        simpleDateFormat.setTimeZone(timeZone);
        return simpleDateFormat.format(calendar.getTime());
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

        public ObservableState(State state) {
            this.state = state;
        }

        private State state;

        private SharedEvent eventToNotify;

        private ObservableState onEvent(Event event){
            final State oldState = state;
            state = state.onEvent(event);
            setChanged();
            eventToNotify = new SharedEvent(event, state, oldState);
            return this;

        }

        public void notifyEvent(){
            if(eventToNotify == null){
                return;
            }
            notifyObservers(eventToNotify);
        }
        State get(){return state;}
    }

    private class DefaultDdiCallback<T> extends LogCallBack<T> {
        @Override
        public void onError(Error error) {
            super.onError(error);
            onEvent(new Event.ErrorEvent(new String[]{error.getMessage()},error.getCode()));
        }

        @Override
        public void onSuccess(T response) {
            super.onSuccess(response);
            onEvent(new Event.SuccessEvent());
        }

        @Override
        public void onFailure(Call<T> call, Throwable t) {
            super.onFailure(call,t);
            onEvent(new Event.FailureEvent(t));
        }
    }

    private class PollingCallback extends DefaultDdiCallback<DdiControllerBase>{
        @Override
        public void onSuccess(DdiControllerBase response) {
            final LinkEntry configDataLink = response.getLink("configData");
            if(configDataLink!=null){
                onEvent(new Event.UpdateConfigRequestEvent());
                return;
            }

            final LinkEntry deploymentBaseLink = response.getLink("deploymentBase");

            if(deploymentBaseLink!=null){
                onEvent(new Event.UpdateFoundEvent(deploymentBaseLink.parseLink().getActionId()));
                return;
            }

            final LinkEntry cancelAction = response.getLink("cancelAction");
            if(cancelAction!=null){
                onEvent(new Event.CancelEvent(cancelAction.parseLink().getActionId()));
                return;
            }

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm:ss");
            long sleepTime = 30_000;
            try {
                simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = simpleDateFormat.parse(response.getConfig().getPolling().getSleep());
                sleepTime = date.getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            onEvent(new Event.SleepEvent(sleepTime));

        }
    }

    private class CheckCancellationCallback extends DefaultDdiCallback<DdiControllerBase>{
        @Override
        public void onSuccess(DdiControllerBase response) {
            final LinkEntry cancelAction = response.getLink("cancelAction");
            if(cancelAction!=null){
                onEvent(new Event.CancelEvent(cancelAction.parseLink().getActionId()));
                return;
            }
            onEvent(new Event.SuccessEvent());
        }
    }

    private class CancelCallback extends DefaultDdiCallback<DdiCancel>{
        @Override
        public void onSuccess(DdiCancel response) {
            onEvent(new Event.SuccessEvent(Long.parseLong(response.getCancelAction().getStopId())));
        }
    }

    private class ControllerBaseDeploymentAction extends DefaultDdiCallback<DdiDeploymentBase>{
        @Override
        public void onSuccess(DdiDeploymentBase response) {
            //super.onSuccess(response);
            onEvent(new Event.DownloadRequestEvent(response));
        }
    }

    private class DownloadArtifact extends DefaultDdiCallback<ResponseBody>{
        private final State.UpdateDownloadState state;

        public DownloadArtifact(State.UpdateDownloadState state) {
            this.state = state;
        }

        @Override
        public void onSuccess(ResponseBody response) {
            //super.onSuccess(response);
            //onEvent(new Event.DownloadRequestEvent(response));
            client.postBasedeploymentActionFeedback(
                    new FeedbackBuilder(state.getActionId(),PROCEEDING,NONE)
                            .withProgess(state.getNextFileToDownload()+1,state.getSize()).build(),
                    tenant,
                    controllerId,
                    state.getActionId())
                    .enqueue(new LogCallBack<>());
            final State.UpdateDownloadState.FileInfo fileInfo = state.getFileInfo();
            onEvent(new Event.FileDownloadedEvent(response.byteStream(),
                    fileInfo.getLinkInfo().getFileName(),
                    fileInfo.getShae1(),
                    fileInfo.getMd5()));
        }
    }

    private ObservableState currentObservableState;
    private String controllerId;
    private String tenant;
    private Timer timer;
    private DdiRestApi client;
    private long retryDelayOnCommunicationError;
}
