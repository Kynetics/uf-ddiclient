package com.kynetics.updatefactory.ddiapiclient.api

import com.kynetics.updatefactory.ddiapiclient.api.model.*
import java.io.InputStream


class DdiClient(private val ddiRestApi: DdiRestApi, private val tenant:String, private val controllerId:String) : IDdiClient {

    override suspend fun getSoftwareModulesArtifacts(softwareModuleId: String): List<ArtfctResp> =
            ddiRestApi.getSoftwareModulesArtifacts(tenant, controllerId, softwareModuleId).await()

    override suspend fun putConfigData(data: CfgDataReq) =
            ddiRestApi.putConfigData(tenant, controllerId, data).await()

    override suspend fun getControllerActions(): CtrlBaseResp =
            ddiRestApi.getControllerActions(tenant, controllerId).await()


    override suspend fun getDeploymentActionDetails(actionId: String, historyCount: Int): DeplBaseResp =
        ddiRestApi.getDeploymentActionDetails(tenant, controllerId, actionId, null, historyCount).await()


    override suspend fun getCancelActionDetails(actionId: String): CnclActResp =
        ddiRestApi.getCancelActionDetails(tenant, controllerId, actionId).await()


    override suspend fun postDeploymentActionFeedback(actionId: String, feedback: DeplFdbkReq) =
            ddiRestApi.postDeploymentActionFeedback(tenant, controllerId, actionId, feedback).await()


    override suspend fun postCancelActionFeedback(actionId: String, feedback: CnclFdbkReq) =
            ddiRestApi.postCancelActionFeedback(tenant, controllerId, actionId, feedback).await()

    override suspend fun downloadArtifact(url: String): InputStream =
            ddiRestApi.downloadArtifact(url).await().byteStream()

}