package com.kynetics.updatefactory.ddiclient.core.test

import com.kynetics.updatefactory.ddiclient.core.UpdateFactoryClientDefaultImpl
import com.kynetics.updatefactory.ddiclient.core.api.*
import com.kynetics.updatefactory.ddiclient.core.api.EventListener
import com.kynetics.updatefactory.ddiclient.core.test.TestUtils.basic
import com.kynetics.updatefactory.ddiclient.core.test.TestUtils.gatewayToken
import com.kynetics.updatefactory.ddiclient.core.test.TestUtils.getDownloadDirectoryFromActionId
import com.kynetics.updatefactory.ddiclient.core.test.TestUtils.tenantName
import com.kynetics.updatefactory.ddiclient.core.test.TestUtils.ufUrl
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.joda.time.Duration
import org.testng.Assert
import java.io.File
import java.util.*

abstract class AbstractClientTest{

    protected  var client: UpdateFactoryClient? = null

    private val queue = LinkedList<() -> Unit >()

    open protected fun defaultClientFromTargetId(
            directoryDataProvider:DirectoryForArtifactsProvider = TestUtils.directoryDataProvider,
            configDataProvider: ConfigDataProvider = TestUtils.configDataProvider,
            updater: Updater = TestUtils.updater,
            eventListeners: List<EventListener> = emptyList(),
            deploymentPermitProvider: DeploymentPermitProvider = object : DeploymentPermitProvider{}): (String) -> UpdateFactoryClient = { targetId ->
        val clientData= UpdateFactoryClientData(
                tenantName,
                targetId,
                ufUrl,
                UpdateFactoryClientData.ServerType.UPDATE_FACTORY,
                gatewayToken )

        val client = UpdateFactoryClientDefaultImpl()

        val eventListener = object : EventListener {
            override fun onEvent(event: EventListener.Event) {
                when (event){

                    is EventListener.Event.UpdateFinished, EventListener.Event.UpdateCancelled -> {
                        queue.poll().invoke()
                    }

                    else -> { println(event) }
                }
            }
        }

        client.init(
                clientData,
                directoryDataProvider,
                configDataProvider,
                deploymentPermitProvider,
                listOf(eventListener, *eventListeners.toTypedArray()),
                updater
        )
        client
    }

    //todo refactor test
    protected fun testTemplate(deployment: TestUtils.TargetDeployments,
                               timeout: Long = Duration.standardSeconds(15).millis,
                               clientFromTargetId: (String) -> UpdateFactoryClient = defaultClientFromTargetId())  = runBlocking{

        withTimeout(timeout){
            client = clientFromTargetId(deployment.targetId)
            val managementApi = ManagementClient.newInstance(ufUrl)

            deployment.deploymentInfo.forEach{ deploymentInfo ->

                var actionStatus = managementApi.getTargetActionStatusAsync(basic, deployment.targetId, deploymentInfo.actionId).await()

                Assert.assertEquals(actionStatus, deploymentInfo.actionStatusOnStart)

                queue.add {
                    launch {
                        delay(400)
                        actionStatus = managementApi.getTargetActionStatusAsync(basic,deployment.targetId,deploymentInfo.actionId).await()

                        Assert.assertEquals(actionStatus.content, deploymentInfo.actionStatusOnFinish.content)

                        deploymentInfo.filesDownloadedPairedWithServerFile.forEach{ (fileDownloaded, serverFile)->
                            println(File(fileDownloaded).absolutePath)
                            println(File(serverFile).absolutePath)
                            Assert.assertEquals(File(fileDownloaded).readText(), File(serverFile).readText())
                        }

                        getDownloadDirectoryFromActionId(deploymentInfo.actionId.toString()).deleteRecursively()
                    }

                }

            }
            client?.startAsync()
            launch {
                while (queue.isNotEmpty()){
                    delay(500)}
            }

        }
    }
}
