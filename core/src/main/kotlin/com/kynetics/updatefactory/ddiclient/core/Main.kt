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
@ObsoleteCoroutinesApi
fun main() = runBlocking {
    System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE")

    var test = false


    val clientData= UpdateFactoryClientData(
            "Default",
            "Target4",
            "http://localhost:8081",
            UpdateFactoryClientData.ServerType.UPDATE_FACTORY,
            "66076ab945a127dd80b15e9011995109")

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
            object : ConfigDataProvider{},
            object : DeploymentPermitProvider{
                override fun downloadAllowed(): Boolean {
                    test = !test
                    return test
                }
                override fun updateAllowed(): Boolean {
                    test = !test
                    return test
                }
            },
            listOf(object : EventListener{
                override fun onEvent(event: EventListener.Event) {
                    println(event)
                }
            }),
            object : Updater { override fun apply(modules: Set<Updater.SwModuleWithPath>, messenger: Updater.Messenger):Boolean {
                println("APPLY UPDATE $modules")
                messenger.sendMessageToServer("Applying the update...")
                Thread.sleep(1000)
                messenger.sendMessageToServer("Update applied")
                return true
            } }
    )
    Thread().run {
        client.startAsync()
        delay(5000)
        client.forcePing()
        delay(5000)
        client.forcePing()
    }
    while(true){}
}