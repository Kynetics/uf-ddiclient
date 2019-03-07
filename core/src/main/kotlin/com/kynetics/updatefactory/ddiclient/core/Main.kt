package com.kynetics.updatefactory.ddiclient.core

import com.kynetics.updatefactory.ddiapiclient.ClientBuilder
import com.kynetics.updatefactory.ddiclient.core.ConnectionManager.Companion.Message.In.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.delay
import java.time.Duration


fun main() = runBlocking {
    val ddiClient =     ClientBuilder()
            .withBaseUrl("https://stage.updatefactory.io")
            .withControllerId("kotlinCtrl")
//            .withGatewayToken("528e9265ab4c7687ca2d8f933d3b77ec")
            .withGatewayToken("sada")
            .build()

    val cm = ConnectionManager.of(GlobalScope.coroutineContext, ddiClient)
    val am = ActionManager.of(GlobalScope.coroutineContext, cm)
    println("start")
    cm.send(Start)
    delay(Duration.ofSeconds(5))
    println("set ping to 3 seconds")
    cm.send(SetPing(Duration.ofSeconds(3)))
    delay(Duration.ofSeconds(10))
    println("unset ping")
    cm.send(SetPing(null))
    delay(Duration.ofMinutes(10))
    println("stop")
    cm.send(Stop)
    delay(Duration.ofSeconds(3))
}