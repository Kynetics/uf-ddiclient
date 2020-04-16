package com.kynetics.updatefactory.ddiclient.virtualdevice

import com.kynetics.updatefactory.ddiclient.core.UpdateFactoryClientDefaultImpl
import com.kynetics.updatefactory.ddiclient.core.api.UpdateFactoryClientData
import com.kynetics.updatefactory.ddiclient.virtualdevice.entrypoint.ConfigDataProviderImpl
import com.kynetics.updatefactory.ddiclient.virtualdevice.entrypoint.DeploymentPermitProviderImpl
import com.kynetics.updatefactory.ddiclient.virtualdevice.entrypoint.DirectoryForArtifactsProviderImpl
import com.kynetics.updatefactory.ddiclient.virtualdevice.entrypoint.MessageListenerImpl
import com.kynetics.updatefactory.ddiclient.virtualdevice.entrypoint.UpdaterImpl
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.random.Random.Default.nextInt
import kotlin.random.Random.Default.nextLong

// TODO add exception handling ! --> A
@ObsoleteCoroutinesApi
fun main() = runBlocking {
    Configuration.apply {
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, logelLevel)

        repeat(poolSize) {
            val clientData = UpdateFactoryClientData(
                tenant,
                controllerIdGenerator.invoke(),
                url,
                UpdateFactoryClientData.ServerType.UPDATE_FACTORY,
                gatewayToken
            )

            GlobalScope.launch {
                val delay = nextLong(0,virtualDeviceStartingDelay)
                println("Virtual Device $it starts in $delay milliseconds")
                delay(delay)
                getClient(clientData, it).startAsync()
            }
        }
    }

    while (true) {}
}

private fun getClient(clientData: UpdateFactoryClientData, virtualDeviceId: Int): UpdateFactoryClientDefaultImpl {
    val client = UpdateFactoryClientDefaultImpl()
    client.init(
        clientData,
        DirectoryForArtifactsProviderImpl(clientData.controllerId),
        ConfigDataProviderImpl(virtualDeviceId, clientData),
        DeploymentPermitProviderImpl(),
        listOf(MessageListenerImpl(virtualDeviceId, clientData)),
        UpdaterImpl(virtualDeviceId, clientData)
    )
    return client
}
