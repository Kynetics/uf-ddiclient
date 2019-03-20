package com.kynetics.updatefactory.ddiapiclient.api

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.kynetics.updatefactory.ddiapiclient.api.model.*
import com.kynetics.updatefactory.ddiapiclient.security.Authentication
import com.kynetics.updatefactory.ddiapiclient.security.HawkbitAuthenticationRequestInterceptor
import com.kynetics.updatefactory.ddiapiclient.security.UpdateFactoryAuthenticationRequestInterceptor
import com.kynetics.updatefactory.ddiclient.core.api.UpdateFactoryClientData
import com.kynetics.updatefactory.ddiclient.core.api.UpdateFactoryClientData.ServerType.HAWKBIT
import okhttp3.OkHttpClient
import org.slf4j.LoggerFactory
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.InputStream
import java.util.*
import java.util.concurrent.Executors


class DdiClientDefaultImpl private constructor(private val ddiRestApi: DdiRestApi, private val tenant:String, private val controllerId:String) : DdiClient {

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
        LOG.debug("getDeploymentActionDetails($actionId, $historyCount)")
        val response = ddiRestApi.getDeploymentActionDetails(tenant, controllerId, actionId, null, historyCount).await()
        if(LOG.isDebugEnabled){
            LOG.debug("$response")
        }
        return response
    }


    override suspend fun getCancelActionDetails(actionId: String): CnclActResp {
        LOG.debug("getCancelActionDetails($actionId)")
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
        ddiRestApi.postDeploymentActionFeedback(tenant, controllerId, actionId, feedback).await()
    }


    override suspend fun postCancelActionFeedback(actionId: String, feedback: CnclFdbkReq) {
        if(LOG.isDebugEnabled){
            LOG.debug("postCancelActionFeedback($actionId, $feedback)")
        }
        ddiRestApi.postCancelActionFeedback(tenant, controllerId, actionId, feedback).await()
    }

    override suspend fun downloadArtifact(url: String): InputStream {
        LOG.debug("downloadArtifact($url)")
        return ddiRestApi.downloadArtifact(url).await().byteStream()
    }

    companion object {
        val LOG = LoggerFactory.getLogger(DdiClient::class.java)!!
        fun of(updateFactoryClientData: UpdateFactoryClientData): DdiClientDefaultImpl {
            val httpBuilder = OkHttpClient.Builder()
            val authentications = HashSet<Authentication>()
            with(updateFactoryClientData){
                if(gatewayToken != null) {
                    authentications.add(Authentication.newInstance(Authentication.AuthenticationType.GATEWAY_TOKEN_AUTHENTICATION, gatewayToken!!))
                }
                if(targetToken != null) {
                    authentications.add(Authentication.newInstance(Authentication.AuthenticationType.TARGET_TOKEN_AUTHENTICATION, targetToken!!))
                }
                httpBuilder.interceptors().add(0, if (serverType == HAWKBIT)
                    HawkbitAuthenticationRequestInterceptor(authentications)
                else
                    UpdateFactoryAuthenticationRequestInterceptor(authentications, targetTokenFoundListener))
                val ddiRestApi = Retrofit.Builder()
                        .baseUrl(serverUrl)
                        .addConverterFactory(GsonConverterFactory.create())
                        .addCallAdapterFactory(CoroutineCallAdapterFactory())
                        .callbackExecutor(Executors.newSingleThreadExecutor())
                        .client(httpBuilder.build())
                        .build()
                        .create(DdiRestApi::class.java)
                return DdiClientDefaultImpl(ddiRestApi, tenant, controllerId)

            }
        }
    }
}