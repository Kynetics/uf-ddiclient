package com.kynetics.updatefactory.ddiapiclient.api

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory

import com.kynetics.updatefactory.ddiapiclient.api.model.*
import com.kynetics.updatefactory.ddiapiclient.security.Authentication
import com.kynetics.updatefactory.ddiapiclient.security.HawkbitAuthenticationRequestInterceptor
import com.kynetics.updatefactory.ddiapiclient.security.UpdateFactoryAuthenticationRequestInterceptor
import com.kynetics.updatefactory.ddiclient.core.api.UpdateFactoryClientData
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.InputStream
import java.util.HashSet
import java.util.concurrent.Executors
import com.kynetics.updatefactory.ddiclient.core.api.UpdateFactoryClientData.ServerType.*


class DdiClientDefaultImpl private constructor(private val ddiRestApi: DdiRestApi, private val tenant:String, private val controllerId:String) : DdiClient {

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

    companion object {
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