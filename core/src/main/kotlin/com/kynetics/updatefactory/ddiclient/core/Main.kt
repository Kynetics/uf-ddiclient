package com.kynetics.updatefactory.ddiclient.core

import com.kynetics.updatefactory.ddiclient.core.api.ConfigDataProvider
import com.kynetics.updatefactory.ddiclient.core.api.DeploymentPermitProvider
import com.kynetics.updatefactory.ddiclient.core.api.DirectoryForArtifactsProvider
import com.kynetics.updatefactory.ddiclient.core.api.MessageListener
import com.kynetics.updatefactory.ddiclient.core.api.UpdateFactoryClientData
import com.kynetics.updatefactory.ddiclient.core.api.Updater
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.joda.time.Duration
import java.io.File
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.UUID

fun File.md5(): String {
    val md = MessageDigest.getInstance("MD5")
    val sb = StringBuilder()

    inputStream().use { fis ->
        val digestInputStream = DigestInputStream(fis, md)
        val buffer = ByteArray(4096)
        while (digestInputStream.read(buffer) > -1) {}
        digestInputStream.close()
        digestInputStream.messageDigest
            .digest()
            .forEach {
                sb.append(String.format("%02X", it))
            }
    }
    return sb.toString().toLowerCase()
}

private fun env(envVariable:String, defaultValue:String):String{
    return System.getenv(envVariable) ?: defaultValue
}

// TODO add exception handling ! --> A
@ObsoleteCoroutinesApi
fun main() = runBlocking {
    System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, env("UF_LOG_LEVEL","TRACE"))
    repeat(env("UF_CLIENT_POOL_SIZE", "1").toInt()) {
        val clientData = UpdateFactoryClientData(
            env("UF_TENANT", "TEST"),
            env("UF_CONTROLLER_ID", UUID.randomUUID().toString()),
            env("UF_URL","https://stage.updatefactory.io"),
            UpdateFactoryClientData.ServerType.UPDATE_FACTORY,
            env("UF_GATEWAY_TOKEN","")
        )

        GlobalScope.launch {
            delay(Duration.standardSeconds(env("UF_VIRTUAL_DEVICE_STARTING_DELAY", "1").toLong()).millis)
            getClient(clientData, it).startAsync()
        }
    }

    while (true) {}
}

private fun getClient(clientData: UpdateFactoryClientData, virtualDeviceId: Int): UpdateFactoryClientDefaultImpl {
    val client = UpdateFactoryClientDefaultImpl()
    client.init(
        clientData,
        object : DirectoryForArtifactsProvider {
            override fun directoryForArtifacts(): File = File("${env("UF_STORAGE_PATH","/client")}/${clientData.controllerId}")
        },
        object : ConfigDataProvider {
            override fun configData(): Map<String, String> {
                return env("UF_TARGET_ATTRIBUTES","test_key,test_value")
                    .split("|")
                    .map { it.split(",").let { list -> list[0] to String.format(list[1], virtualDeviceId, clientData.tenant, clientData.controllerId, clientData.gatewayToken) } }
                    .toMap()
            }
        },
        object : DeploymentPermitProvider {
            override fun downloadAllowed(): Deferred<Boolean> {
                return CompletableDeferred(true)
            }

            override fun updateAllowed(): Deferred<Boolean> {
                return CompletableDeferred(true)
            }
        },
        listOf(object : MessageListener {
            override fun onMessage(message: MessageListener.Message) {
                println(String.format(env("UF_LOG_MESSAGE", "\${4}"),
                    virtualDeviceId,
                    clientData.tenant,
                    clientData.controllerId,
                    clientData.gatewayToken,message))
            }
        }),
        object : Updater {
            override fun apply(
                modules: Set<Updater.SwModuleWithPath>,
                messenger: Updater.Messenger
            ): Updater.UpdateResult {
                println("APPLY UPDATE $modules")
                val regex = Regex("VIRTUAL_DEVICE_UPDATE_RESULT_(\\*|${clientData.controllerId})")
                val result = modules.fold (Pair(true, listOf<String>())) { acc, module->

                    val command = (module.metadata?.firstOrNull{md -> md.key.contains(regex)}?.value ?: "OK|1|").split("|")

                    messenger.sendMessageToServer(
                        String.format(env("UF_SRV_MSF_BEFORE_UPDATE", "Applying the sw {0} for target {1}"),
                            module.name,
                            virtualDeviceId,
                            clientData.tenant,
                            clientData.controllerId,
                            clientData.gatewayToken)
                    )
                    Thread.sleep(command[1].toLong() * 1000)
                    messenger.sendMessageToServer(
                        String.format(env("UF_SRV_MSF_AFTER_UPDATE", "Applied the sw {0} for target {1}"),
                            module.name,
                            virtualDeviceId,
                            clientData.tenant,
                            clientData.controllerId,
                            clientData.gatewayToken)
                    )

                    (acc.first && command[0] == "OK") to (acc.second + command.drop(2))
                }


                return Updater.UpdateResult(result.first, result.second)
            }
        }
    )
    return client
}
