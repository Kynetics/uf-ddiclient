/*
 * Copyright © 2017-2019 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.kynetics.updatefactory.ddiapiclient.api

import com.kynetics.updatefactory.ddiapiclient.api.model.*
import kotlinx.coroutines.Deferred
import okhttp3.ResponseBody
import retrofit2.http.*

/**
 * REST resource handling for root controller CRUD operations.
 *
 * @author Daniele Sergio
 */
interface DdiRestApi {

    /**
     * Returns all artifacts of a given software module and target.
     *
     * @param tenant
     * of the client
     * @param controllerId
     * of the target that matches to controller id
     * @param softwareModuleId
     * of the software module
     * @return the response
     */
    @Headers("Accept: application/hal+json")
    @GET(DdiRestConstants.BASE_V1_REQUEST_MAPPING + "/{controllerId}/softwaremodules/{softwareModuleId}/artifacts")
    fun getSoftwareModulesArtifacts(@Path("tenant") tenant: String,
                                    @Path("controllerId") controllerId: String,
                                    @Path("softwareModuleId") softwareModuleId: Long?): Deferred<List<DdiArtifact>>

    /**
     * Root resource for an individual [Target].
     *
     * @param tenant
     * of the request
     * @param controllerId
     * of the target that matches to controller id
     * @return the response
     */
    @Headers("Accept: application/hal+json")
    @GET(DdiRestConstants.BASE_V1_REQUEST_MAPPING + "/{controllerId}")
    fun getControllerActions(@Path("tenant") tenant: String,
                             @Path("controllerId") controllerId: String): Deferred<DdiControllerBase>

    /**
     * Handles GET [DdiArtifact] download request.
     *
     * @param url
     */
    @Streaming
    @GET
    fun downloadArtifact(@Url url: String): Deferred<ResponseBody>

    /**
     * Resource for software module.
     *
     * @param tenant
     * of the request
     * @param controllerId
     * of the target
     * @param actionId
     * of the [DdiDeploymentBase] that matches to active
     * actions.
     * @param resource
     * an hashcode of the resource which indicates if the action has
     * been changed, e.g. from 'soft' to 'force' and the eTag needs
     * to be re-generated
     * @param actionHistoryMessageCount
     * specifies the number of messages to be returned from action
     * history.
     * actionHistoryMessageCount < 0, retrieves the maximum allowed
     * number of action status messages from history;
     * actionHistoryMessageCount = 0, does not retrieve any message;
     * and actionHistoryMessageCount > 0, retrieves the specified
     * number of messages, limited by maximum allowed number.
     * @return the response
     */
    @Headers("Accept: application/hal+json")
    @GET(DdiRestConstants.BASE_V1_REQUEST_MAPPING + "/{controllerId}/" + DdiRestConstants.DEPLOYMENT_BASE_ACTION
            + "/{actionId}")
    fun getDeploymentActionDetails(@Path("tenant") tenant: String,
                                   @Path("controllerId") controllerId: String,
                                   @Path("actionId") actionId: Long,
                                   @Query(value = "c") resource: Int?,
                                   @Query(value = "actionHistory") actionHistoryMessageCount: Int?): Deferred<DdiDeploymentBase>

    /**
     * This is the feedback channel for the [DdiDeploymentBase] action.
     *
     * @param tenant
     * of the client
     * @param feedback
     * to provide
     * @param controllerId
     * of the target that matches to controller id
     * @param actionId
     * of the action we have feedback for
     *
     * @return the response
     */
    @Headers("Content-Type: application/json")
    @POST(DdiRestConstants.BASE_V1_REQUEST_MAPPING + "/{controllerId}/" + DdiRestConstants.DEPLOYMENT_BASE_ACTION + "/{actionId}/"
            + DdiRestConstants.FEEDBACK)
    fun postDeploymentActionFeedback(@Path("tenant") tenant: String,
                                     @Path("controllerId") controllerId: String,
                                     @Path("actionId") actionId: Long?,
                                     @Body feedback: DdiActionFeedback): Deferred<Void>

    /**
     * This is the feedback channel for the config data action.
     *
     * @param tenant
     * of the client
     * @param configData
     * as body
     * @param controllerId
     * to provide data for
     *
     * @return status of the request
     */
    @Headers("Content-Type: application/json")
    @PUT(value = DdiRestConstants.BASE_V1_REQUEST_MAPPING + "/{controllerId}/"
            + DdiRestConstants.CONFIG_DATA_ACTION)
    fun putConfigData(@Path("tenant") tenant: String,
                      @Path("controllerId") controllerId: String,
                      @Body configData: DdiConfigData): Deferred<Void>

    /**
     * RequestMethod.GET method for the [DdiCancel] action.
     *
     * @param tenant
     * of the request
     * @param controllerId
     * ID of the calling target
     * @param actionId
     * of the action
     *
     * @return the [DdiCancel] response
     */
    @Headers("Accept: application/hal+json")
    @GET(value = DdiRestConstants.BASE_V1_REQUEST_MAPPING + "/{controllerId}/" + DdiRestConstants.CANCEL_ACTION
            + "/{actionId}")
    fun getCancelActionDetails(@Path("tenant") tenant: String,
                               @Path("controllerId") controllerId: String,
                               @Path("actionId") actionId: Long): Deferred<DdiCancel>

    /**
     * RequestMethod.POST method receiving the [DdiActionFeedback] from
     * the target.
     *
     * @param tenant
     * of the client
     * @param feedback
     * the [DdiActionFeedback] from the target.
     * @param controllerId
     * the ID of the calling target
     * @param actionId
     * of the action we have feedback for
     *
     * @return the [DdiActionFeedback] response
     */

    @Headers("Content-Type: application/json")
    @POST(DdiRestConstants.BASE_V1_REQUEST_MAPPING + "/{controllerId}/" + DdiRestConstants.CANCEL_ACTION + "/{actionId}/"
            + DdiRestConstants.FEEDBACK)
    fun postCancelActionFeedback(@Path("tenant") tenant: String,
                                 @Path("controllerId") controllerId: String,
                                 @Path("actionId") actionId: Long?,
                                 @Body feedback: DdiActionFeedback): Deferred<Void>
}

