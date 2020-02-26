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




suspend fun delay(duration: Duration) = delay(duration.millis)

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
    System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE")

    (0 .. env("UF_CLIENT_POOL_SIZE", "1").toInt()).forEach {
        val clientData = UpdateFactoryClientData(
            env("UF_TENANT", "TEST"),
            env("UF_CONTROLLER_ID", UUID.randomUUID().toString()),
            env("UF_URL","https://stage.updatefactory.io"),
            UpdateFactoryClientData.ServerType.UPDATE_FACTORY,
            env("UF_GATEWAY_TOKEN","")
        )

        GlobalScope.launch {
            getClient(clientData).startAsync()
        }
    }

    while (true) {}
}

private fun getClient(clientData: UpdateFactoryClientData): UpdateFactoryClientDefaultImpl {
    var test1 = false
    val client = UpdateFactoryClientDefaultImpl()
    client.init(
        clientData,
        object : DirectoryForArtifactsProvider {
            override fun directoryForArtifacts(): File = File(".${clientData.controllerId}")
        },
        object : ConfigDataProvider {},
        object : DeploymentPermitProvider {
            override fun downloadAllowed(): Deferred<Boolean> {
                test1 = !test1
                return CompletableDeferred(test1)
            }

            override fun updateAllowed(): Deferred<Boolean> {
                test1 = !test1
                return CompletableDeferred(test1)
            }
        },
        listOf(object : MessageListener {
            override fun onMessage(message: MessageListener.Message) {
                println(message)
            }
        }),
        object : Updater {
            override fun apply(
                modules: Set<Updater.SwModuleWithPath>,
                messenger: Updater.Messenger
            ): Updater.UpdateResult {
                println("APPLY UPDATE $modules")
                messenger.sendMessageToServer("Applying the update...")
                Thread.sleep(1000)
                messenger.sendMessageToServer("Update applied")
                return Updater.UpdateResult(true)
            }
        }
    )
    return client
}
