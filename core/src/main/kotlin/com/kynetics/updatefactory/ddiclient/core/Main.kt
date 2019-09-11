package com.kynetics.updatefactory.ddiclient.core

import com.kynetics.updatefactory.ddiclient.core.api.ConfigDataProvider
import com.kynetics.updatefactory.ddiclient.core.api.DeploymentPermitProvider
import com.kynetics.updatefactory.ddiclient.core.api.DirectoryForArtifactsProvider
import com.kynetics.updatefactory.ddiclient.core.api.MessageListener
import com.kynetics.updatefactory.ddiclient.core.api.UpdateFactoryClientData
import com.kynetics.updatefactory.ddiclient.core.api.Updater
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.joda.time.Duration
import java.io.File
import java.security.DigestInputStream
import java.security.MessageDigest

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

// TODO add exception handling ! --> A
@ObsoleteCoroutinesApi
fun main() = runBlocking {
    System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE")

    var test = false

    val clientData = UpdateFactoryClientData(
            "ABA",
            "test-rc2",
            "https://stage.updatefactory.io",
            UpdateFactoryClientData.ServerType.UPDATE_FACTORY,
            "4625fea04ef2dea436c9fe342dbc2586")

/*
    val clientData= UpdateFactoryClientData(
            "test",
            "Target4",
            "https://stage.updatefactory.io",
            UpdateFactoryClientData.ServerType.UPDATE_FACTORY,
            "528e9265ab4c7687ca2d8f933d3b77ec")
*/
    val client = UpdateFactoryClientDefaultImpl()
    client.init(
            clientData,
            object : DirectoryForArtifactsProvider { override fun directoryForArtifacts(): File = File(".") },
            object : ConfigDataProvider {},
            object : DeploymentPermitProvider {
                override fun downloadAllowed(): Deferred<Boolean> {
                    test = !test
                    return CompletableDeferred(test)
                }
                override fun updateAllowed(): Deferred<Boolean> {
                    test = !test
                    return CompletableDeferred(test)
                }
            },
            listOf(object : MessageListener {
                override fun onMessage(message: MessageListener.Message) {
                    println(message)
                }
            }),
            object : Updater { override fun apply(modules: Set<Updater.SwModuleWithPath>, messenger: Updater.Messenger): Updater.UpdateResult {
                println("APPLY UPDATE $modules")
                messenger.sendMessageToServer("Applying the update...")
                Thread.sleep(1000)
                messenger.sendMessageToServer("Update applied")
                return Updater.UpdateResult(true)
            } }
    )
    Thread().run {
        client.startAsync()
        delay(5000)
        client.forcePing()
        delay(5000)
        client.forcePing()
    }
    while (true) {}
}
