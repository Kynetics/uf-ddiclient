package com.kynetics.updatefactory.ddiapiclient.api

import com.kynetics.updatefactory.ddiapiclient.api.model.*
import java.io.InputStream

interface IDdiClient {

    suspend fun getControllerActions(): CtrlBaseResp

    suspend fun getDeploymentActionDetails(actionId: String, historyCount: Int = -1): DeplBaseResp

    suspend fun getCancelActionDetails(actionId: String): CnclActResp

    suspend fun getSoftwareModulesArtifacts(softwareModuleId: String): List<ArtfctResp>

    suspend fun postDeploymentActionFeedback(actionId: String, feedback: DeplFdbkReq): Unit

    suspend fun postCancelActionFeedback(actionId: String, feedback: CnclFdbkReq): Unit

    suspend fun putConfigData(data: CfgDataReq): Unit

    suspend fun downloadArtifact(url: String): InputStream
}