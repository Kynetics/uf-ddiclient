package com.kynetics.updatefactory.ddiclient.core

import com.kynetics.updatefactory.ddiapiclient.ClientBuilder
import com.kynetics.updatefactory.ddiapiclient.api.model.LinkEntry
import com.kynetics.updatefactory.ddiapiclient.api.model.LinkType
import com.kynetics.updatefactory.ddiclient.core.ConnectionManager.Companion.Message.In.*
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
    val am = ActionManager.of(GlobalScope.coroutineContext, cm)
    println("start")
    cm.send(Start)
    delay(Duration.ofMinutes(5))
}