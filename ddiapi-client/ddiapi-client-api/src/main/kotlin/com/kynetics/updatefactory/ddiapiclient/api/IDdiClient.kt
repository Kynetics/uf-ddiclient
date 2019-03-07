package com.kynetics.updatefactory.ddiapiclient.api

import com.kynetics.updatefactory.ddiapiclient.api.model.DdiActionFeedback
import com.kynetics.updatefactory.ddiapiclient.api.model.DdiCancel
import com.kynetics.updatefactory.ddiapiclient.api.model.DdiControllerBase
import com.kynetics.updatefactory.ddiapiclient.api.model.DdiDeploymentBase

interface IDdiClient {

    suspend fun getControllerActions(): DdiControllerBase

    suspend fun getDeploymentActionDetails(actionId: Long, historyCount: Int = -1): DdiDeploymentBase

    suspend fun getCancelActionDetails(actionId: Long): DdiCancel

    suspend fun postDeploymentActionFeedback(actionId: Long, feedback: DdiActionFeedback): Void

    suspend fun postCancelActionFeedback(actionId: Long, feedback: DdiActionFeedback): Void
}