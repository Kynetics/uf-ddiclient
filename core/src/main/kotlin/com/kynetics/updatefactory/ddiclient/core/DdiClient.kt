package com.kynetics.updatefactory.ddiclient.core

import com.kynetics.updatefactory.ddiapiclient.api.DdiRestApi
import com.kynetics.updatefactory.ddiapiclient.model.request.DdiActionFeedback
import com.kynetics.updatefactory.ddiapiclient.model.response.DdiCancel
import com.kynetics.updatefactory.ddiapiclient.model.response.DdiControllerBase
import com.kynetics.updatefactory.ddiapiclient.model.response.DdiDeploymentBase

class DdiClient(private val ddiRestApi: DdiRestApi, private val tenant:String, private val controllerId:String) {

    suspend fun getControllerActions():DdiControllerBase = ddiRestApi.getControllerActions(tenant, controllerId).await()


    suspend fun getDeploymentActionDetails(actionId: Long, historyCount: Int = -1):DdiDeploymentBase =
        ddiRestApi.getDeploymentActionDetails(tenant, controllerId, actionId, null, historyCount).await()


    suspend fun getCancelActionDetails(actionId: Long):DdiCancel =
        ddiRestApi.getCancelActionDetails(tenant, controllerId, actionId).await()


    suspend fun postDeploymentActionFeedback(actionId: Long, feedback: DdiActionFeedback) =
        ddiRestApi.postDeploymentActionFeedback(tenant, controllerId, actionId, feedback).await()


    suspend fun postCancelActionFeedback(actionId: Long, feedback: DdiActionFeedback) =
        ddiRestApi.postCancelActionFeedback(tenant, controllerId, actionId, feedback).await()


}