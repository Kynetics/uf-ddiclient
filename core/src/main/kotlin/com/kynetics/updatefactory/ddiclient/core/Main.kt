package com.kynetics.updatefactory.ddiclient.core

import com.kynetics.updatefactory.ddiapiclient.ClientBuilder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.delay
import okhttp3.OkHttpClient
import java.time.Duration

fun main() = runBlocking<Unit> {
    val builder = ClientBuilder()
            .withHttpBuilder(OkHttpClient.Builder())
            .withBaseUrl("https://stage.updatefactory.io")
            .withGatewayToken("528e9265ab4c7687ca2d8f933d3b77ec")
//            .withGatewayToken("")

    val cm = ConnectionManager.of(GlobalScope.coroutineContext, DdiClient(builder.build(), "test", "kotlinCtrl"))
    println("start")
    cm.send(ConnectionManager.Companion.Message.Start)
    delay(Duration.ofSeconds(5))
    println("set ping to 3 seconds")
    cm.send(ConnectionManager.Companion.Message.SetPing(Duration.ofSeconds(3)))
    delay(Duration.ofSeconds(10))
    println("unset ping")
    cm.send(ConnectionManager.Companion.Message.SetPing(null))
    delay(Duration.ofMinutes(10))
    println("stop")
    cm.send(ConnectionManager.Companion.Message.Stop)
    delay(Duration.ofSeconds(3))
}