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
import com.kynetics.updatefactory.ddiclient.core.filterinputstream.CheckFilterInputStream;
import com.kynetics.updatefactory.ddiclient.core.filterinputstream.NotifyStatusFilterInputStream;
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
import com.kynetics.updatefactory.ddiclient.core.servicecallback.SystemOperation;
import com.kynetics.updatefactory.ddiclient.core.servicecallback.UserInteraction;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

import static com.kynetics.updatefactory.ddiclient.api.model.request.DdiResult.FinalResult.*;
import static com.kynetics.updatefactory.ddiclient.api.model.request.DdiStatus.ExecutionStatus.*;
import static com.kynetics.updatefactory.ddiclient.core.servicecallback.SystemOperation.StatusName.NOT_APPLIED;
import static com.kynetics.updatefactory.ddiclient.core.servicecallback.SystemOperation.StatusName.SUCCESSFULLY_APPLIED;

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
              SystemOperation systemOperation,
              UserInteraction userInteraction,
              long retryDelayOnCommunicationError){
        if(initialState.getStateName() == AbstractState.StateName.SAVING_FILE &&
                (!((SavingFileState)initialState).isInputStreamAvailable())){
            initialState = new WaitingState(0, null);
        }
        currentObservableState = new ObservableState(initialState);
        this.client = client;
        this.retryDelayOnCommunicationError = retryDelayOnCommunicationError;
        this.tenant = tenant;
        this.controllerId = controllerId;
        this.targetData = targetData;
        this.systemOperation = systemOperation;
        this.userInteraction = userInteraction;
        process();
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
        abortRequest();
    }

    public void restart(){
        stop();
        start();
    }

    public void addObserver(Observer observer){
        currentObservableState.addObserver(observer);
    }

    private void setUpdateSucceffullyUpdate(SystemOperation.UpdateStatus updateStatus, AbstractState state){
        checkServiceRunning();
        if(!currentObservableState.get().getStateName().equals(AbstractState.StateName.UPDATE_STARTED)){
            throw new IllegalStateException("current state must be UPDATE_STARTED to call this method");
        }
        onEvent(new WrapperEvent(updateStatus.getStatusName() == SUCCESSFULLY_APPLIED ?
                new SuccessEvent() :
                new UpdateErrorEvent(updateStatus.getMessages()), state));
    }

    private void checkServiceRunning(){
        if(timer == null){
            throw  new IllegalStateException("service isn't yet started");
        }
    }

    private void setAuthorized(AuthorizationWaitingState state, boolean isAuthorized){
        checkServiceRunning();
        if(!currentObservableState.get().getStateName().equals(AbstractState.StateName.AUTHORIZATION_WAITING)){
            throw new IllegalStateException("current state must be UPDATE_STARTED to call this method");
        }
        onEvent(new WrapperEvent(isAuthorized ? new AuthorizationGrantedEvent() : new AuthorizationDeniedEvent(), state));
    }

    public void restartSuspendState(){
        checkServiceRunning();
        if(!currentObservableState.get().getStateName().equals(AbstractState.StateName.WAITING)){
            throw new IllegalStateException("current state must be WAITING to call this method");
        }
        timer.cancel();
        timer.purge();
        timer = new Timer();
        onEvent(new WrapperEvent(new ResumeEvent(), currentObservableState.get()));
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

    private void onEvent(WrapperEvent event){
        try {
            events.put(event);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void process(){
        new Thread(() -> {
            while(true) {
                try {
                    final WrapperEvent event = events.take();
                    final boolean isProcess = currentObservableState.onEvent(event);
                    if(isProcess){
                        handlerState(currentObservableState.get());
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    private void handlerState(AbstractState currentState, long forceDelay){
        switch (currentState.getStateName()){
            case WAITING:
                abortRequest();
                final Call controllerBaseCall =  client.getControllerBase(tenant, controllerId);
                final WaitingState waitingState = ((WaitingState)currentState);
                forceDelay = waitingState.innerStateIsCommunicationState() ? LAST_SLEEP_TIME_FOUND : forceDelay;
                execute(controllerBaseCall, new PollingCallback(currentState), forceDelay > 0 ? forceDelay : waitingState.getSleepTime());
                break;
            case CONFIG_DATA:
                final Call call = getConfigDataCall();
                execute(call, new DefaultDdiCallback(currentState), forceDelay);
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
                execute(baseDeploymentAction, new ControllerBaseDeploymentAction(currentState), forceDelay);
                break;
            case UPDATE_DOWNLOAD:
                final UpdateDownloadState updateDownloadState = (UpdateDownloadState) currentState;
                long spaceNeeded = 0;
                for(int i = updateDownloadState.getNextFileToDownload(); i<updateDownloadState.getFileInfoList().size(); i++){
                    spaceNeeded += updateDownloadState.getFileInfoList().get(i).getSize();
                }

                if(systemOperation.checkSpace(spaceNeeded)){
                    final Call callToAbort = client.downloadArtifact(
                            tenant,
                            controllerId,
                            updateDownloadState.getFileInfo().getLinkInfo().getSoftwareModules(),
                            updateDownloadState.getFileInfo().getLinkInfo().getFileName());
                    try {
                        callsToAbort.put(callToAbort);
                        execute(callToAbort, new DownloadArtifact(updateDownloadState), forceDelay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        onEvent(new WrapperEvent(new ErrorEvent(new String[]{"Interrupted Exception", e.getMessage()},1), currentState));
                    }
                } else {
                    onEvent(new WrapperEvent(new InsufficientSpaceEvent(), currentState));
                }

                break;
            case UPDATE_READY:
                final Call checkCancellation = client.getControllerBase(tenant, controllerId);
                execute(checkCancellation,new CheckCancellationCallback(currentState), forceDelay);
                break;
            case CANCELLATION_CHECK:
                final CancellationCheckState cancellationCheckState = (CancellationCheckState)currentState;
                final Call controllerCancelAction = client.getControllerCancelAction(tenant,controllerId,cancellationCheckState.getActionId());
                execute(controllerCancelAction, new CancelCallback(currentState), forceDelay);
                break;
            case CANCELLATION:
                abortRequest();
                final CancellationState cancellationState = (CancellationState)currentState;
                DdiActionFeedback actionFeedback = new FeedbackBuilder(cancellationState.getActionId(),CLOSED, SUCESS).build();
                final Call postCancelActionFeedback = client.postCancelActionFeedback(actionFeedback, tenant,controllerId,cancellationState.getActionId());
                execute(postCancelActionFeedback, new DefaultDdiCallback(currentState), forceDelay);
                break;
            case SAVING_FILE:
                final SavingFileState savingFileState = (SavingFileState) currentState;
                if(savingFileState.isInputStreamAvailable()){
                    new Thread(() -> {
                        final CheckFilterInputStream streamWithChecker = CheckFilterInputStream.builder()
                                .withStream(savingFileState.getInputStream())
                                .withMd5Value(savingFileState.getFileInfo().getHash().getMd5())
                                .withSha1Value(savingFileState.getFileInfo().getHash().getSha1())
                                .withListener(new FileCheckerListenerImpl(savingFileState))
                                .build();

                        final String fileName = savingFileState.getFileInfo().getLinkInfo().getFileName();
                        final NotifyStatusFilterInputStream stream = new NotifyStatusFilterInputStream(
                                streamWithChecker,
                                savingFileState.getFileInfo().getSize(),
                                new ServerNotifier(client, 10, savingFileState.getActionId(), tenant, controllerId, fileName, savingFileState)
                        );
                        systemOperation.savingFile(stream, savingFileState.getFileInfo());
                    }).start();
                }
                execute(client.getControllerBase(tenant, controllerId), new CheckCancelEventCallback(currentState, new DownloadPendingEvent()),30_000);
                break;
            case UPDATE_STARTED:
                final UpdateStartedState updateStartedState = (UpdateStartedState)currentState;
                final SystemOperation.UpdateStatus updateStatus = systemOperation.updateStatus();
                if( updateStatus.getStatusName() == NOT_APPLIED){
                    systemOperation.executeUpdate(updateStartedState.getActionId());
                }
                setUpdateSucceffullyUpdate(systemOperation.updateStatus(), currentState);
                break;
            case AUTHORIZATION_WAITING:
                final AuthorizationWaitingState authorizationWaitingState = (AuthorizationWaitingState) currentState;
                if(!authorizationWaitingState.isRequestSend()) {
                    authorizationWaitingState.sendRequest();
                    final AbstractState.StateName innerStateName = authorizationWaitingState.getState().getStateName();
                    new Thread(() -> {
                        Boolean authorization = Boolean.FALSE;
                        try {
                            authorization = userInteraction.grantAuthorization(
                                    innerStateName == AbstractState.StateName.UPDATE_DOWNLOAD ? UserInteraction.Authorization.DOWNLOAD : UserInteraction.Authorization.UPDATE)
                                    .get();
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                        setAuthorized(authorizationWaitingState, authorization);
                    }
                    ).start();
                }
                execute(client.getControllerBase(tenant, controllerId), new CheckCancelEventCallback(currentState, new AuthorizationPendingEvent()),LAST_SLEEP_TIME_FOUND);

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
                execute(lastFeedbackCall,new LastFeedbackCallback(currentState), forceDelay);
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

                execute(serverErrorFeedbackCall,new DefaultDdiCallback(currentState), forceDelay);
                break;
            case COMMUNICATION_ERROR:
            case COMMUNICATION_FAILURE:
                handlerState(((AbstractStateWithInnerState)currentState).getState(), retryDelayOnCommunicationError);
                break;
        }
        currentObservableState.notifyEvent();
    }

    private void abortRequest() {
        System.out.println("start abort pending requests");
        while(!callsToAbort.isEmpty()){
            System.out.println("Execute abort");
            try {
                callsToAbort.take().cancel();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("finish abort pending requests");
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

    private static class LogCallBack<T> extends DdiCallback<T>{
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

    private class LastFeedbackCallback<T> extends DefaultDdiCallback<T>{

        public LastFeedbackCallback(AbstractState state) {
            super(state);
        }

        @Override
        public void onError(Error error) {
            if(error.getCode() == 410){
                onEvent(new SuccessEvent());
            } else {
                super.onError(error);
            }
        }
    }
    private class ObservableState extends Observable{

        public ObservableState(AbstractState state) {
            this.state = state;
        }

        private AbstractState state;

        private SharedEvent eventToNotify;

        private boolean onEvent(WrapperEvent wrappedEvent){
            System.out.println(String.format("Event(%s) is related to state with id %s, current state id is %s",
                    wrappedEvent.getEvent().getEventName(), wrappedEvent.getState(), state));
            if(state != wrappedEvent.getState()){
                System.out.println("Event is ignored because it is out of sync");
                return false;
            }
            final AbstractEvent event = wrappedEvent.getEvent();
            final AbstractState oldState = state;
            state = state.onEvent(event);
            setChanged();
            eventToNotify = new SharedEvent(event, state, oldState);
            return true;

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
        protected final AbstractState state;

        public DefaultDdiCallback(AbstractState state) {
            this.state = state;
        }

        protected void onEvent(AbstractEvent event){
            UFService.this.onEvent(new WrapperEvent(event, state));
        }

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

        public PollingCallback(AbstractState state) {
            super(state);
        }

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

    private class CheckCancelEventCallback extends DefaultDdiCallback<DdiControllerBase>{

        private final AbstractEvent event;

        public CheckCancelEventCallback(AbstractState state, AbstractEvent event) {
            super(state);
            this.event = event;
        }

        @Override
        public void onError(Error error) {
            if(error.getCode() == 410){
                onEvent(new ForceCancelEvent());
            } else {
               super.onError(error);
            }
        }

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

            final LinkEntry cancelAction = response.getLink("cancelAction");
            if(cancelAction!=null){
                onEvent(new CancelEvent(cancelAction.parseLink().getActionId()));
                return;
            }

            final LinkEntry configDataLink = response.getLink("configData");
            if(configDataLink!=null){
                onEvent(new ForceCancelEvent());
                return;
            }

            final LinkEntry deploymentBaseLink = response.getLink("deploymentBase");
            if(deploymentBaseLink==null){
                onEvent(new ForceCancelEvent());
                return;
            }

            onEvent(event);
        }
    }

    private class CheckCancellationCallback extends DefaultDdiCallback<DdiControllerBase>{
        public CheckCancellationCallback(AbstractState state) {
            super(state);
        }

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
        public CancelCallback(AbstractState state) {
            super(state);
        }

        @Override
        public void onSuccess(DdiCancel response) {
            onEvent(new SuccessEvent(Long.parseLong(response.getCancelAction().getStopId())));
        }
    }

    private class ControllerBaseDeploymentAction extends DefaultDdiCallback<DdiDeploymentBase>{
        public ControllerBaseDeploymentAction(AbstractState state) {
            super(state);
        }

        @Override
        public void onSuccess(DdiDeploymentBase response) {
            //super.onSuccess(response);
            onEvent(new DownloadRequestEvent(response));
        }
    }

    private class DownloadArtifact extends DefaultDdiCallback<ResponseBody>{
        private final UpdateDownloadState downloadState;

        public DownloadArtifact(UpdateDownloadState state) {
            super(state);
            this.downloadState = state;
        }

        @Override
        public void onSuccess(ResponseBody response) {
            new Thread(() -> {
                client.postBasedeploymentActionFeedback(
                        new FeedbackBuilder(downloadState.getActionId(),PROCEEDING,NONE)
                                .withProgess(downloadState.getNextFileToDownload()+1,downloadState.getSize()).build(),
                        tenant,
                        controllerId,
                        downloadState.getActionId())
                        .enqueue(new LogCallBack<>());
                final String fileName = downloadState.getFileInfo().getLinkInfo().getFileName();
                onEvent(new DownloadStartedEvent(response.byteStream(),fileName));
            }).start();

        }
    }

    private class ServerNotifier implements NotifyStatusFilterInputStream.Notifier{
        private int lastNotify = 0;
        private final DdiRestApi client;
        private final int notifyThreshold;
        private final long actionId;
        private final String tenant;
        private final String controllerId;
        private final String fileName;
        private final AbstractState state;

        public ServerNotifier(DdiRestApi client, int notifyThreshold, long actionId, String tenant, String controllerId, String fileName, AbstractState currentState) {
            this.client = client;
            this.notifyThreshold = notifyThreshold;
            this.actionId = actionId;
            this.tenant = tenant;
            this.controllerId = controllerId;
            this.fileName = fileName;
            this.state = currentState;
        }

        @Override
        public void notify(double percent) {
            final int per = (int) Math.floor(percent * 100);
            if(per / notifyThreshold != lastNotify / notifyThreshold){
                lastNotify = per;
                final String message = String.format("Downloading %s - %s%%", fileName, lastNotify);
                System.out.println(message);
                List<String> details = new ArrayList<>(1);
                details.add(message);
                client.postBasedeploymentActionFeedback(
                        new FeedbackBuilder(actionId,PROCEEDING,NONE)
                                .withDetails(details).build(),
                        tenant,
                        controllerId,
                        actionId)
                        .enqueue(new LogCallBack<Void>(){
                            @Override
                            public void onError(Error error) {
                                if(currentObservableState.get().getStateName() != state.getStateName()){
                                    return;
                                }
                                if(error.getCode() == 410 || error.getCode() == 404){
                                    onEvent(new WrapperEvent(new ForceCancelEvent(),state));
                                } else {
                                    super.onError(error);
                                }
                            }
                        });
            }
        }
    }

    private class FileCheckerListenerImpl implements CheckFilterInputStream.FileCheckListener{
        final AbstractState state;

        public FileCheckerListenerImpl(AbstractState state) {
            this.state = state;
        }

        @Override
        public void onValidationResult(boolean isValid, Hash hash) {
            onEvent(new WrapperEvent(isValid ? new SuccessEvent() : new FileCorruptedEvent(hash), state ));
        }
    }

    private ObservableState currentObservableState;
    private BlockingQueue<Call> callsToAbort = new ArrayBlockingQueue<>(50);
    private TargetData targetData;
    private String controllerId;
    private String tenant;
    private Timer timer;
    private DdiRestApi client;
    private long retryDelayOnCommunicationError;
    private final SystemOperation systemOperation;
    private final UserInteraction userInteraction;
    private BlockingQueue<WrapperEvent> events = new ArrayBlockingQueue<>(5);

    private static class WrapperEvent{
        final AbstractEvent event;
        final AbstractState state;

        public WrapperEvent(AbstractEvent event, AbstractState state) {
            this.event = event;
            this.state = state;
        }

        public AbstractEvent getEvent() {
            return event;
        }

        public Object getState() {
            return state;
        }
    }
}
