package com.kynetics.updatefactory.ddiclient.core

import com.kynetics.updatefactory.ddiapiclient.ClientBuilder
import com.kynetics.updatefactory.ddiclient.core.ConnectionManager.Companion.Message.In.Start
import com.kynetics.updatefactory.ddiclient.core.api.ConfigDataProvider
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.delay
import java.time.Duration


fun main() = runBlocking {
    val ddiClient =     ClientBuilder()
            .withBaseUrl("http://localhost:8081")
            .withControllerId("target3")
            .withGatewayToken("66076ab945a127dd80b15e9011995109")
            .withTetnat("Default")
//            .withGatewayToken("sada")
            .build()

    val cm = ConnectionManager.of(GlobalScope.coroutineContext, ddiClient)
    val am = ActionManager.of(GlobalScope.coroutineContext, cm, UpdaterRegistry(), ConfigDataProvider.DEFAULT_INSTACE,  ddiClient)
    println("start")
    cm.send(Start)
    delay(Duration.ofSeconds(6))
    println("set ping 3s")
    cm.send(ConnectionManager.Companion.Message.In.SetPing(Duration.ofSeconds(3)))
    delay(Duration.ofSeconds(30))
    println("unset ping")
    cm.send(ConnectionManager.Companion.Message.In.SetPing(null))
    delay(Duration.ofMinutes(6))
    println("set ping 3s")
    cm.send(ConnectionManager.Companion.Message.In.SetPing(Duration.ofSeconds(3)))
    delay(Duration.ofSeconds(30))
    println("stop")
    cm.send(ConnectionManager.Companion.Message.In.Stop)
    delay(Duration.ofSeconds(10))
    println("exit")
    //cm.close()
}