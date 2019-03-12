package com.kynetics.updatefactory.ddiclient.core

import com.kynetics.updatefactory.ddiapiclient.ClientBuilder
import com.kynetics.updatefactory.ddiclient.core.ConnectionManager.Companion.Message.In.Start
import com.kynetics.updatefactory.ddiclient.core.api.TestUpdater
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.joda.time.Duration

suspend fun delay(duration:Duration)=delay(duration.millis)

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