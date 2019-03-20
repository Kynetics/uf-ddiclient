package com.kynetics.updatefactory.ddiclient.core

import com.kynetics.updatefactory.ddiclient.core.api.*
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.joda.time.Duration
import java.io.File
import java.security.DigestInputStream
import java.security.MessageDigest

suspend fun delay(duration:Duration)=delay(duration.millis)

fun File.md5():String{
    val md = MessageDigest.getInstance("MD5")
    val sb = StringBuilder()

    inputStream().use { fis ->
        val digestInputStream = DigestInputStream(fis, md)
        val buffer = ByteArray(4096)
        while (digestInputStream.read(buffer) > -1){}
        digestInputStream.close()
        digestInputStream.messageDigest
                .digest()
                .forEach {
                    sb.append(String.format("%02X", it))
                }
    }
    return sb.toString().toLowerCase()
}

//TODO add exception handling ! --> A
//TODO add updater confirmation check for download and installaion ! --> D
//TODO apply returns with messages ! --> D
//TODO add event notification
//TODO manage ETAG & co. --> A
@ObsoleteCoroutinesApi
fun main() = runBlocking {
    System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE")

    val clientData= UpdateFactoryClientData(
            "Default",
            "target3",
            "http://localhost:8081",
            UpdateFactoryClientData.ServerType.UPDATE_FACTORY,
            "66076ab945a127dd80b15e9011995109")

    val client = UpdateFactoryClientDefaultImpl()
    client.init(
            clientData,
            object : DirectoryForArtifactsProvider { override fun directoryForArtifacts(actionId: String): File = File("./$actionId") },
            object : ConfigDataProvider{},
            object : DeploymentPermitProvider{
                override fun downloadAllowed(): Boolean = true
                override fun updateAllowed(): Boolean = true
            },
            object : Updater { override fun apply(modules: Set<Updater.SwModuleWithPath>, messanger: Updater.Messanger):Boolean {
                println("APPLY UPDATE $modules")
                messanger.sendMessageToServer("Applying the update...")
                Thread.sleep(1000)
                messanger.sendMessageToServer("Update applied")
                return true
            } }
    )
    println("start")
    client.startAsync()
    delay(Duration.standardSeconds(5))
    println("force ping")
    repeat(3) {
        client.forcePing()
        delay(Duration.standardSeconds(3))
    }
    delay(Duration.standardSeconds(5))
    println("exit")
    client.stop()
    delay(Duration.standardSeconds(3))
    System.exit(0)
}