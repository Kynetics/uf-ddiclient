package com.kynetics.updatefactory.ddiclient.core

import com.kynetics.updatefactory.ddiclient.core.api.ConfigDataProvider
import com.kynetics.updatefactory.ddiclient.core.api.DirectoryForArtifactsProvider
import com.kynetics.updatefactory.ddiclient.core.api.UpdateFactoryClientData
import com.kynetics.updatefactory.ddiclient.core.api.Updater
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

//TODO add logging ! --> D
//TODO add exception handling ! --> A
//TODO add updater confirmation check for download and installaion ! --> D
//TODO apply returns with messages ! --> D
//TODO add event notification
@ObsoleteCoroutinesApi
fun main() = runBlocking {
    val clientData= UpdateFactoryClientData(
            "Default",
            "target3",
            "http://localhost:8081",
            UpdateFactoryClientData.ServerType.UPDATE_FACTORY,
            "66076ab945a127dd80b15e9011995109")

    val client = UpdateFactoryClientDefaultImpl()
    client.init(
            clientData,
            object : DirectoryForArtifactsProvider { override fun directoryForArtifacts(actionId: String): File = File(".") },
            object : ConfigDataProvider{},
            object : Updater { override fun apply(modules: Set<Updater.SwModuleWithPath>) { println("APPLY UPDATE $modules")}}
    )
    println("start")
    client.startAsync()
    delay(Duration.standardSeconds(10))
    println("force ping")
    client.forcePing()
    delay(Duration.standardSeconds(10))
    println("exit")
    client.stop()
    delay(Duration.standardSeconds(1))
}