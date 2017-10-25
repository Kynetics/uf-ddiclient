/*
 * Copyright © 2017 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.kynetics.updatefactory.ddiclient.api.api;

import com.kynetics.updatefactory.ddiclient.api.model.request.DdiActionFeedback;
import com.kynetics.updatefactory.ddiclient.api.model.request.DdiConfigData;
import com.kynetics.updatefactory.ddiclient.api.model.response.DdiArtifact;
import com.kynetics.updatefactory.ddiclient.api.model.response.DdiCancel;
import com.kynetics.updatefactory.ddiclient.api.model.response.DdiControllerBase;
import com.kynetics.updatefactory.ddiclient.api.model.response.DdiDeploymentBase;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

import java.lang.annotation.Target;
import java.util.List;

/**
 * REST resource handling for root controller CRUD operations.
 *
 *  @author Daniele Sergio
 */
public interface DdiRestApi {

    /**
     * Returns all artifacts of a given software module and target.
     *
     * @param tenant
     *            of the client
     * @param controllerId
     *            of the target that matches to controller id
     * @param softwareModuleId
     *            of the software module
     * @return the response
     */
    @Headers("Accept: application/hal+json")
    @GET(DdiRestConstants.BASE_V1_REQUEST_MAPPING +
            "/{controllerId}/softwaremodules/{softwareModuleId}/artifacts")
    Call<List<DdiArtifact>> getSoftwareModulesArtifacts(@Path("tenant") final String tenant,
                                                        @Path("controllerId") final String controllerId,
                                                        @Path("softwareModuleId") final Long softwareModuleId);

    /**
     * Root resource for an individual {@link Target}.
     *
     * @param tenant
     *            of the request
     * @param controllerId
     *            of the target that matches to controller id
     * @return the response
     */
    @Headers("Accept: application/hal+json")
    @GET(DdiRestConstants.BASE_V1_REQUEST_MAPPING + "/{controllerId}")
    Call<DdiControllerBase> getControllerBase(@Path("tenant") final String tenant,
                                              @Path("controllerId") final String controllerId);

    /**
     * Handles GET {@link DdiArtifact} download request. This could be full or
     * partial (as specified by RFC7233 (Range Requests)) download request.
     *
     * @param tenant
     *            of the request
     * @param controllerId
     *            of the target
     * @param softwareModuleId
     *            of the parent software module
     * @param fileName
     *            of the related local artifact
     */
    @GET(DdiRestConstants.BASE_V1_REQUEST_MAPPING + "/{controllerId}/softwaremodules/{softwareModuleId}/artifacts/{fileName}")
    Call<ResponseBody> downloadArtifact(@Path("tenant") final String tenant,
                                        @Path("controllerId") final String controllerId,
                                        @Path("softwareModuleId") final Long softwareModuleId,
                                        @Path("fileName") final String fileName);

    //    /**
//     * Handles GET {@link DdiArtifact} download request. This could be full or
//     * partial (as specified by RFC7233 (Range Requests)) download request.
//     *
//     * @param tenant
//     *            of the request
//     * @param controllerId
//     *            of the target
//     * @param softwareModuleId
//     *            of the parent software module
//     * @param fileName
//     *            of the related local artifact
//     * @param response
//     *            of the servlet
//     * @param request
//     *            from the client
//     *
//     * @return response of the servlet which in case of success is status code
//     *         {@link HttpStatus#OK} or in case of partial download
//     *         {@link HttpStatus#PARTIAL_CONTENT}.
//     */
//    @RequestMapping(method = RequestMethod.GET, value = "/{controllerId}/softwaremodules/{softwareModuleId}/artifacts/{fileName}")
//    ResponseEntity<InputStream> downloadArtifact(@PathVariable("tenant") final String tenant,
//                                                 @PathVariable("controllerId") final String controllerId,
//                                                 @PathVariable("softwareModuleId") final Long softwareModuleId,
//                                                 @PathVariable("fileName") final String fileName);

//    /**
//     * Handles GET {@link DdiArtifact} MD5 checksum file download request.
//     *
//     * @param tenant
//     *            of the request
//     * @param controllerId
//     *            of the target
//     * @param softwareModuleId
//     *            of the parent software module
//     * @param fileName
//     *            of the related local artifact
//     * @param response
//     *            of the servlet
//     * @param request
//     *            the HTTP request injected by spring
//     *
//     * @return {@link ResponseEntity} with status {@link HttpStatus#OK} if
//     *         successful
//     */
//    @RequestMapping(method = RequestMethod.GET, value = "/{controllerId}/softwaremodules/{softwareModuleId}/artifacts/{fileName}"
//            + DdiRestConstants.ARTIFACT_MD5_DWNL_SUFFIX, produces = MediaType.TEXT_PLAIN_VALUE)
//    ResponseEntity<Void> downloadArtifactMd5(@PathVariable("tenant") final String tenant,
//                                             @PathVariable("controllerId") final String controllerId,
//                                             @PathVariable("softwareModuleId") final Long softwareModuleId,
//                                             @PathVariable("fileName") final String fileName);

    /**
     * Resource for software module.
     *
     * @param tenant
     *            of the request
     * @param controllerId
     *            of the target
     * @param actionId
     *            of the {@link DdiDeploymentBase} that matches to active
     *            actions.
     * @param resource
     *            an hashcode of the resource which indicates if the action has
     *            been changed, e.g. from 'soft' to 'force' and the eTag needs
     *            to be re-generated
     * @param actionHistoryMessageCount
     *            specifies the number of messages to be returned from action
     *            history.
     *            actionHistoryMessageCount < 0, retrieves the maximum allowed
     *            number of action status messages from history;
     *            actionHistoryMessageCount = 0, does not retrieve any message;
     *            and actionHistoryMessageCount > 0, retrieves the specified
     *            number of messages, limited by maximum allowed number.
     * @return the response
     */
    @Headers("Accept: application/hal+json")
    @GET(DdiRestConstants.BASE_V1_REQUEST_MAPPING + "/{controllerId}/" + DdiRestConstants.DEPLOYMENT_BASE_ACTION
            + "/{actionId}")
    Call<DdiDeploymentBase> getControllerBasedeploymentAction(@Path("tenant") final String tenant,
                                                              @Path("controllerId") final String controllerId,
                                                              @Path("actionId") final Long actionId,
                                                              @Query(value = "c") final int resource,
                                                              @Query(value = "actionHistory") final Integer actionHistoryMessageCount);

    /**
     * This is the feedback channel for the {@link DdiDeploymentBase} action.
     *
     * @param tenant
     *            of the client
     * @param feedback
     *            to provide
     * @param controllerId
     *            of the target that matches to controller id
     * @param actionId
     *            of the action we have feedback for
     *
     * @return the response
     */
    @Headers("Content-Type: application/json")
    @POST(DdiRestConstants.BASE_V1_REQUEST_MAPPING + "/{controllerId}/" + DdiRestConstants.DEPLOYMENT_BASE_ACTION + "/{actionId}/"
            + DdiRestConstants.FEEDBACK)
    Call<Void> postBasedeploymentActionFeedback(@Body final DdiActionFeedback feedback,
                                                          @Path("tenant") final String tenant, @Path("controllerId") final String controllerId,
                                                          @Path("actionId") final Long actionId);

    /**
     * This is the feedback channel for the config data action.
     *
     * @param tenant
     *            of the client
     * @param configData
     *            as body
     * @param controllerId
     *            to provide data for
     *
     * @return status of the request
     */
    @Headers("Content-Type: application/json")
    @PUT(value = DdiRestConstants.BASE_V1_REQUEST_MAPPING + "/{controllerId}/"
            + DdiRestConstants.CONFIG_DATA_ACTION)
    Call<Void> putConfigData(@Body final DdiConfigData configData,
                                       @Path("tenant") final String tenant, @Path("controllerId") final String controllerId);

    /**
     * RequestMethod.GET method for the {@link DdiCancel} action.
     *
     * @param tenant
     *            of the request
     * @param controllerId
     *            ID of the calling target
     * @param actionId
     *            of the action
     *
     * @return the {@link DdiCancel} response
     */
    @Headers("Accept: application/hal+json")
    @GET(value = DdiRestConstants.BASE_V1_REQUEST_MAPPING + "/{controllerId}/" + DdiRestConstants.CANCEL_ACTION
            + "/{actionId}")
    Call<DdiCancel> getControllerCancelAction(@Path("tenant") final String tenant,
                                              @Path("controllerId") final String controllerId,
                                              @Path("actionId") final Long actionId);

    /**
     * RequestMethod.POST method receiving the {@link DdiActionFeedback} from
     * the target.
     *
     * @param tenant
     *            of the client
     * @param feedback
     *            the {@link DdiActionFeedback} from the target.
     * @param controllerId
     *            the ID of the calling target
     * @param actionId
     *            of the action we have feedback for
     *
     * @return the {@link DdiActionFeedback} response
     */

    @Headers("Content-Type: application/json")
    @POST(DdiRestConstants.BASE_V1_REQUEST_MAPPING + "/{controllerId}/" + DdiRestConstants.CANCEL_ACTION + "/{actionId}/"
            + DdiRestConstants.FEEDBACK)
    Call<Void> postCancelActionFeedback(@Body final DdiActionFeedback feedback,
                                                  @Path("tenant") final String tenant,
                                                  @Path("controllerId") final String controllerId,
                                                  @Path("actionId") final Long actionId);
}
