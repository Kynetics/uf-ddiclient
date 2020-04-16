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