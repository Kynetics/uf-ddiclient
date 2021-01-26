/*
 * Copyright © 2017-2021  Kynetics  LLC
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package com.kynetics.updatefactory.ddiclient.virtualdevice.entrypoint

import com.kynetics.updatefactory.ddiclient.core.api.MessageListener
import com.kynetics.updatefactory.ddiclient.core.api.UpdateFactoryClientData
import com.kynetics.updatefactory.ddiclient.virtualdevice.Configuration
import java.text.MessageFormat

class MessageListenerImpl(
    private val virtualDeviceId:Int,
    private val clientData: UpdateFactoryClientData
): MessageListener {
    override fun onMessage(message: MessageListener.Message) {
        println(
            MessageFormat.format(
            Configuration.logMessageTemplate,
            virtualDeviceId,
            clientData.tenant,
            clientData.controllerId,
            clientData.gatewayToken,message))
    }
}