/*
 * Copyright © 2017-2021  Kynetics  LLC
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package com.kynetics.updatefactory.ddiclient.core.api

import java.net.MalformedURLException
import java.net.URL

data class UpdateFactoryClientData constructor(
    val tenant: String,
    val controllerId: String,
    val serverUrl: String,
    val serverType: ServerType = ServerType.UPDATE_FACTORY,
    val gatewayToken: String? = null,
    val targetToken: String? = null,
    val targetTokenFoundListener: TargetTokenFoundListener? = null
) {

    init {
        notEmpty(tenant, "tenant")
        notEmpty(controllerId, "controllerId")
        notEmpty(serverUrl, "serverUrl")
        validUrl(serverUrl, "serverUrl")
        if ((gatewayToken == null || gatewayToken.isBlank()) && (targetToken == null || targetToken.isBlank())) {
            throw IllegalStateException("gatewayToken and targetToken cannot both be empty")
        }
        if (targetTokenFoundListener != null && serverType == ServerType.HAWKBIT) {
            throw IllegalStateException("targetTokenFoundListener can only be used with the UPDATE_FACTORY serverType")
        }
    }

    private fun notEmpty(item: String, itemName: String) {
        if (item.isBlank()) {
            throw IllegalArgumentException("$itemName could not be null or empty")
        }
    }

    private fun validUrl(url: String, itemName: String) {
        try {
            URL(url)
        } catch (e: MalformedURLException) {
            throw IllegalArgumentException("$itemName is a malformed url")
        }
    }

    enum class ServerType { UPDATE_FACTORY, HAWKBIT }
}
