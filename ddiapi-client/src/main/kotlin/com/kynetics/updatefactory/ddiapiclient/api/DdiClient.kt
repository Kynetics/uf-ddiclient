package com.kynetics.updatefactory.ddiapiclient.api

import com.kynetics.updatefactory.ddiapiclient.api.model.DdiActionFeedback
import com.kynetics.updatefactory.ddiapiclient.api.model.DdiCancel
import com.kynetics.updatefactory.ddiapiclient.api.model.DdiControllerBase
import com.kynetics.updatefactory.ddiapiclient.api.model.DdiDeploymentBase


class DdiClient(private val ddiRestApi: DdiRestApi, private val tenant:String, private val controllerId:String) : IDdiClient {

    override suspend fun getControllerActions(): DdiControllerBase = ddiRestApi.getControllerActions(tenant, controllerId).await()


    override suspend fun getDeploymentActionDetails(actionId: Long, historyCount: Int): DdiDeploymentBase =
        ddiRestApi.getDeploymentActionDetails(tenant, controllerId, actionId, null, historyCount).await()


    override suspend fun getCancelActionDetails(actionId: Long): DdiCancel =
        ddiRestApi.getCancelActionDetails(tenant, controllerId, actionId).await()


    override suspend fun postDeploymentActionFeedback(actionId: Long, feedback: DdiActionFeedback) =
        ddiRestApi.postDeploymentActionFeedback(tenant, controllerId, actionId, feedback).await()


    override suspend fun postCancelActionFeedback(actionId: Long, feedback: DdiActionFeedback) =
        ddiRestApi.postCancelActionFeedback(tenant, controllerId, actionId, feedback).await()

}