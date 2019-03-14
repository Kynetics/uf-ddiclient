package com.kynetics.updatefactory.ddiapiclient.api

import com.kynetics.updatefactory.ddiapiclient.api.model.*
import org.slf4j.LoggerFactory
import java.io.InputStream


class DdiClient(private val ddiRestApi: DdiRestApi, private val tenant:String, private val controllerId:String) : IDdiClient {

    override suspend fun getSoftwareModulesArtifacts(softwareModuleId: String): List<ArtfctResp>{
        LOG.debug("getSoftwareModulesArtifacts($softwareModuleId)")
        val artifact = ddiRestApi.getSoftwareModulesArtifacts(tenant, controllerId, softwareModuleId).await()
        if(LOG.isDebugEnabled){
            LOG.debug("$artifact")
        }
        return artifact
    }


    override suspend fun putConfigData(data: CfgDataReq){
        LOG.debug("putConfigData($CfgDataReq)")
        ddiRestApi.putConfigData(tenant, controllerId, data).await()
    }


    override suspend fun getControllerActions(): CtrlBaseResp{
        LOG.debug("getControllerActions()")
        val response = ddiRestApi.getControllerActions(tenant, controllerId).await()
        if(LOG.isDebugEnabled){
            LOG.debug("$response")
        }
        return response
    }



    override suspend fun getDeploymentActionDetails(actionId: String, historyCount: Int): DeplBaseResp {
        LOG.debug("getControllerActions($actionId, $historyCount)")
        val response = ddiRestApi.getDeploymentActionDetails(tenant, controllerId, actionId, null, historyCount).await()
        if(LOG.isDebugEnabled){
            LOG.debug("$response")
        }
        return response
    }


    override suspend fun getCancelActionDetails(actionId: String): CnclActResp {
        LOG.debug("getControllerActions($actionId)")
        val response = ddiRestApi.getCancelActionDetails(tenant, controllerId, actionId).await()
        if(LOG.isDebugEnabled){
            LOG.debug("$response")
        }
        return response
    }


    override suspend fun postDeploymentActionFeedback(actionId: String, feedback: DeplFdbkReq) {
        if(LOG.isDebugEnabled){
            LOG.debug("postDeploymentActionFeedback($actionId, $feedback)")
        }
        val response = ddiRestApi.postDeploymentActionFeedback(tenant, controllerId, actionId, feedback).await()
        if(LOG.isDebugEnabled){
            LOG.debug("$response")
        }
        return response

    }


    override suspend fun postCancelActionFeedback(actionId: String, feedback: CnclFdbkReq) {
        if(LOG.isDebugEnabled){
            LOG.debug("postCancelActionFeedback($actionId, $feedback)")
        }
        val response = ddiRestApi.postCancelActionFeedback(tenant, controllerId, actionId, feedback).await()
        if(LOG.isDebugEnabled){
            LOG.debug("$response")
        }
        return response
    }

    override suspend fun downloadArtifact(url: String): InputStream {
        LOG.debug("downloadArtifact($url)")
        return ddiRestApi.downloadArtifact(url).await().byteStream()
    }

    companion object {
        val LOG = LoggerFactory.getLogger(DdiClient::class.java)
    }
}