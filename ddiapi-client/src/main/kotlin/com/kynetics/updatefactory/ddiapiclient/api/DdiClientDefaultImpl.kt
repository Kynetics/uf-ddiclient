/*
 * Copyright © 2017-2021  Kynetics  LLC
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package com.kynetics.updatefactory.ddiapiclient.api

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.kynetics.updatefactory.ddiapiclient.api.model.ArtfctResp
import com.kynetics.updatefactory.ddiapiclient.api.model.CfgDataReq
import com.kynetics.updatefactory.ddiapiclient.api.model.CnclActResp
import com.kynetics.updatefactory.ddiapiclient.api.model.CnclFdbkReq
import com.kynetics.updatefactory.ddiapiclient.api.model.CtrlBaseResp
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplBaseResp
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplFdbkReq
import com.kynetics.updatefactory.ddiapiclient.security.Authentication
import com.kynetics.updatefactory.ddiapiclient.security.HawkbitAuthenticationRequestInterceptor
import com.kynetics.updatefactory.ddiapiclient.security.UpdateFactoryAuthenticationRequestInterceptor
import com.kynetics.updatefactory.ddiclient.core.api.TargetTokenFoundListener
import com.kynetics.updatefactory.ddiclient.core.api.UpdateFactoryClientData
import com.kynetics.updatefactory.ddiclient.core.api.UpdateFactoryClientData.ServerType.HAWKBIT
import java.io.InputStream
import java.net.HttpURLConnection
import java.util.HashSet
import java.util.concurrent.Executors
import okhttp3.OkHttpClient
import org.slf4j.LoggerFactory
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class DdiClientDefaultImpl private constructor(private val ddiRestApi: DdiRestApi, private val tenant: String, private val controllerId: String) : DdiClient {

    override suspend fun getSoftwareModulesArtifacts(softwareModuleId: String): List<ArtfctResp> {
        LOG.debug("getSoftwareModulesArtifacts({})", softwareModuleId)
        val artifact = ddiRestApi.getSoftwareModulesArtifacts(tenant, controllerId, softwareModuleId).await()
        LOG.debug("{}", artifact)
        return artifact
    }

    override suspend fun putConfigData(data: CfgDataReq, onSuccessConfigData: () -> Unit) {
        LOG.debug("putConfigData({})", CfgDataReq)
        val responseCode = ddiRestApi.putConfigData(tenant, controllerId, data).await().code()
        if (responseCode in 200 until 300) {
            onSuccessConfigData.invoke()
        }
    }

    override suspend fun getControllerActions(): CtrlBaseResp {
        LOG.debug("getControllerActions()")
        val response = ddiRestApi.getControllerActions(tenant, controllerId).await()
        LOG.debug("{}", response)
        return handleResponse(response)
    }

    override suspend fun onControllerActionsChange(etag: String, onChange: OnResourceChange<CtrlBaseResp>) {
        LOG.debug("onDeploymentActionDetailsChange({})", etag)
        val response = ddiRestApi.getControllerActions(tenant, controllerId, etag).await()
        LOG.debug("{}", response)
        handleOnChangeResponse(response, etag, "BaseResource", onChange)
    }

    override suspend fun getDeploymentActionDetails(actionId: String, historyCount: Int): DeplBaseResp {
        LOG.debug("getDeploymentActionDetails($actionId, $historyCount)")
        val response = ddiRestApi.getDeploymentActionDetails(tenant, controllerId, actionId, null, historyCount).await()
        LOG.debug("{}", response)
        return handleResponse(response)
    }

    override suspend fun onDeploymentActionDetailsChange(actionId: String, historyCount: Int, etag: String, onChange: OnResourceChange<DeplBaseResp>) {
        LOG.debug("onDeploymentActionDetailsChange($actionId, $historyCount, $etag)")
        val response = ddiRestApi.getDeploymentActionDetails(tenant, controllerId, actionId, null, historyCount, etag).await()
        LOG.debug("{}", response)
        handleOnChangeResponse(response, etag, "Deployment", onChange)
    }

    override suspend fun getCancelActionDetails(actionId: String): CnclActResp {
        LOG.debug("getCancelActionDetails($actionId)")
        val response = ddiRestApi.getCancelActionDetails(tenant, controllerId, actionId).await()
        LOG.debug("{}", response)
        return response
    }

    override suspend fun postDeploymentActionFeedback(actionId: String, feedback: DeplFdbkReq) {
        LOG.debug("postDeploymentActionFeedback({},{})", actionId, feedback)
        ddiRestApi.postDeploymentActionFeedback(tenant, controllerId, actionId, feedback).await()
    }

    override suspend fun postCancelActionFeedback(actionId: String, feedback: CnclFdbkReq) {
        LOG.debug("postCancelActionFeedback({},{})", actionId, feedback)
        ddiRestApi.postCancelActionFeedback(tenant, controllerId, actionId, feedback).await()
    }

    override suspend fun downloadArtifact(url: String): InputStream {
        LOG.debug("downloadArtifact({})", url)
        return ddiRestApi.downloadArtifact(url).await().byteStream()
    }

    private suspend fun <T> handleOnChangeResponse(response: Response<T>, etag: String, resourceName: String, onChange: OnResourceChange<T>) {
        when (response.code()) {
            in 200..299 -> {
                val newEtag = response.headers()[ETAG_HEADER] ?: ""
                LOG.info("{} is changed. Old ETag: {}, new ETag: {}", resourceName, etag, newEtag)
                onChange.invoke(response.body()!!, newEtag)
            }

            HttpURLConnection.HTTP_NOT_MODIFIED -> LOG.info("{} not changed", resourceName)

            else -> throw HttpException(response)
        }
    }

    private fun <T> handleResponse(response: Response<T>): T {
        return when (response.code()) {
            in 200..299 -> response.body()!!
            else -> throw HttpException(response)
        }
    }

    companion object {
        const val ETAG_HEADER = "ETag"

        val LOG = LoggerFactory.getLogger(DdiClient::class.java)!!

        fun of(updateFactoryClientData: UpdateFactoryClientData): DdiClientDefaultImpl {
            val httpBuilder = OkHttpClient.Builder()
            val authentications = HashSet<Authentication>()
            with(updateFactoryClientData) {
                if (gatewayToken != null) {
                    authentications.add(Authentication.newInstance(Authentication.AuthenticationType.GATEWAY_TOKEN_AUTHENTICATION, gatewayToken!!))
                }
                if (targetToken != null) {
                    authentications.add(Authentication.newInstance(Authentication.AuthenticationType.TARGET_TOKEN_AUTHENTICATION, targetToken!!))
                }
                httpBuilder.interceptors().add(0, if (serverType == HAWKBIT)
                    HawkbitAuthenticationRequestInterceptor(authentications)
                else
                    UpdateFactoryAuthenticationRequestInterceptor(authentications, targetTokenFoundListener ?: object : TargetTokenFoundListener {}))
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
