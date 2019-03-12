package com.kynetics.updatefactory.ddiclient.core

import com.kynetics.updatefactory.ddiapiclient.ClientBuilder
import com.kynetics.updatefactory.ddiclient.core.ConnectionManager.Companion.Message.In.Start
import com.kynetics.updatefactory.ddiclient.core.api.TestUpdater
import kotlinx.coroutines.GlobalScope
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

@ObsoleteCoroutinesApi
fun main() = runBlocking {
    val ddiClient =     ClientBuilder()
            .withBaseUrl("http://localhost:8081")
            .withControllerId("target3")
            .withGatewayToken("66076ab945a127dd80b15e9011995109")
            .withTetnat("Default")
//            .withGatewayToken("sada")
            .build()
    val updater = TestUpdater(".")
    val registry = UpdaterRegistry(updater)
    val cm = ConnectionManager.of(GlobalScope.coroutineContext, ddiClient)
    ActionManager.of(GlobalScope.coroutineContext,
            cm,
            registry,
            updater,
            ddiClient)
    println("start")
    cm.send(Start)
    delay(Duration.standardSeconds(6))
    println("set ping 3s")
    cm.send(ConnectionManager.Companion.Message.In.SetPing(Duration.standardSeconds(3)))
    delay(Duration.standardSeconds(30))
    println("unset ping")
    cm.send(ConnectionManager.Companion.Message.In.SetPing(null))
    delay(Duration.standardMinutes(6))
    println("set ping 3s")
    cm.send(ConnectionManager.Companion.Message.In.SetPing(Duration.standardSeconds(3)))
    delay(Duration.standardSeconds(30))
    println("stop")
    cm.send(ConnectionManager.Companion.Message.In.Stop)
    delay(Duration.standardSeconds(10))
    println("exit")
    //cm.close()
}