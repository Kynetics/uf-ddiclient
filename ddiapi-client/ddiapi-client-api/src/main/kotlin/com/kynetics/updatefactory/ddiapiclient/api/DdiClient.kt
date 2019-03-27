package com.kynetics.updatefactory.ddiapiclient.api

import com.kynetics.updatefactory.ddiapiclient.api.model.*
import java.io.InputStream

typealias OnResourceChange<T> = suspend (T, String) -> Unit

interface DdiClient {

    suspend fun getControllerActions(): CtrlBaseResp

    suspend fun onControllerActionsChange(etag: String = "", onChange :OnResourceChange<CtrlBaseResp>)

    suspend fun getDeploymentActionDetails(actionId: String, historyCount: Int = -1): DeplBaseResp

    suspend fun onDeploymentActionDetailsChange(actionId: String, historyCount: Int = -1, etag: String = "", onChange :OnResourceChange<DeplBaseResp>)

    suspend fun getCancelActionDetails(actionId: String): CnclActResp

    suspend fun getSoftwareModulesArtifacts(softwareModuleId: String): List<ArtfctResp>

    suspend fun postDeploymentActionFeedback(actionId: String, feedback: DeplFdbkReq)

    suspend fun postCancelActionFeedback(actionId: String, feedback: CnclFdbkReq)

    suspend fun putConfigData(data: CfgDataReq)

    suspend fun downloadArtifact(url: String): InputStream
}