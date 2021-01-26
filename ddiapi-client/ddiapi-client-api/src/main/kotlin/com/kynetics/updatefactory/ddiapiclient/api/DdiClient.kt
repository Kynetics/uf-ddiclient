/*
 * Copyright © 2017-2021  Kynetics  LLC
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package com.kynetics.updatefactory.ddiapiclient.api

import com.kynetics.updatefactory.ddiapiclient.api.model.ArtfctResp
import com.kynetics.updatefactory.ddiapiclient.api.model.CfgDataReq
import com.kynetics.updatefactory.ddiapiclient.api.model.CnclActResp
import com.kynetics.updatefactory.ddiapiclient.api.model.CnclFdbkReq
import com.kynetics.updatefactory.ddiapiclient.api.model.CtrlBaseResp
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplBaseResp
import com.kynetics.updatefactory.ddiapiclient.api.model.DeplFdbkReq
import java.io.InputStream

typealias OnResourceChange<T> = suspend (T, String) -> Unit

interface DdiClient {

    suspend fun getControllerActions(): CtrlBaseResp

    suspend fun onControllerActionsChange(etag: String = "", onChange: OnResourceChange<CtrlBaseResp>)

    suspend fun getDeploymentActionDetails(actionId: String, historyCount: Int = -1): DeplBaseResp

    suspend fun onDeploymentActionDetailsChange(actionId: String, historyCount: Int = -1, etag: String = "", onChange: OnResourceChange<DeplBaseResp>)

    suspend fun getCancelActionDetails(actionId: String): CnclActResp

    suspend fun getSoftwareModulesArtifacts(softwareModuleId: String): List<ArtfctResp>

    suspend fun postDeploymentActionFeedback(actionId: String, feedback: DeplFdbkReq)

    suspend fun postCancelActionFeedback(actionId: String, feedback: CnclFdbkReq)

    suspend fun putConfigData(data: CfgDataReq, onSuccessConfigData: () -> Unit)

    suspend fun downloadArtifact(url: String): InputStream
}
