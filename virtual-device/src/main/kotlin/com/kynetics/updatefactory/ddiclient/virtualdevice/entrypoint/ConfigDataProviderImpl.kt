package com.kynetics.updatefactory.ddiclient.virtualdevice.entrypoint

import com.kynetics.updatefactory.ddiclient.core.api.ConfigDataProvider
import com.kynetics.updatefactory.ddiclient.core.api.UpdateFactoryClientData
import com.kynetics.updatefactory.ddiclient.virtualdevice.Configuration
import java.text.MessageFormat

class ConfigDataProviderImpl(
    private val virtualDeviceId:Int,
    private val clientData: UpdateFactoryClientData): ConfigDataProvider {
    override fun configData(): Map<String, String> {
        return Configuration.targetAttributes
            .split("|")
            .map { it.split(",").let { list -> list[0] to MessageFormat.format(list[1],
                virtualDeviceId,
                clientData.tenant,
                clientData.controllerId,
                clientData.gatewayToken) }
            }
            .toMap()
    }
}