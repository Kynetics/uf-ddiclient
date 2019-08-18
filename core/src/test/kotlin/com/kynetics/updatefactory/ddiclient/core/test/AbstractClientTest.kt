package com.kynetics.updatefactory.ddiclient.core.test

import com.kynetics.updatefactory.ddiclient.core.UpdateFactoryClientDefaultImpl
import com.kynetics.updatefactory.ddiclient.core.api.ConfigDataProvider
import com.kynetics.updatefactory.ddiclient.core.api.DeploymentPermitProvider
import com.kynetics.updatefactory.ddiclient.core.api.DirectoryForArtifactsProvider
import com.kynetics.updatefactory.ddiclient.core.api.MessageListener
import com.kynetics.updatefactory.ddiclient.core.api.UpdateFactoryClient
import com.kynetics.updatefactory.ddiclient.core.api.UpdateFactoryClientData
import com.kynetics.updatefactory.ddiclient.core.api.Updater
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
import java.util.LinkedList

abstract class AbstractClientTest {

    protected var client: UpdateFactoryClient? = null

    private val queue = LinkedList<() -> Unit >()

    protected open fun defaultClientFromTargetId(
        directoryDataProvider: DirectoryForArtifactsProvider = TestUtils.directoryDataProvider,
        configDataProvider: ConfigDataProvider = TestUtils.configDataProvider,
        updater: Updater = TestUtils.updater,
        messageListeners: List<MessageListener> = emptyList(),
        deploymentPermitProvider: DeploymentPermitProvider = object : DeploymentPermitProvider {}
    ): (String) -> UpdateFactoryClient = { targetId ->
        val clientData = UpdateFactoryClientData(
                tenantName,
                targetId,
                ufUrl,
                UpdateFactoryClientData.ServerType.UPDATE_FACTORY,
                gatewayToken)

        val client = UpdateFactoryClientDefaultImpl()

        val eventListener = object : MessageListener {
            override fun onMessage(message: MessageListener.Message) {
                when (message) {

                    is MessageListener.Message.Event.UpdateFinished, MessageListener.Message.State.CancellingUpdate -> {
                        queue.poll().invoke()
                    }

                    else -> { println(message) }
                }
            }
        }

        client.init(
                clientData,
                directoryDataProvider,
                configDataProvider,
                deploymentPermitProvider,
                listOf(eventListener, *messageListeners.toTypedArray()),
                updater
        )
        client
    }

    // todo refactor test
    protected fun testTemplate(
        deployment: TestUtils.TargetDeployments,
        timeout: Long = Duration.standardSeconds(15).millis,
        clientFromTargetId: (String) -> UpdateFactoryClient = defaultClientFromTargetId()
    ) = runBlocking {

        withTimeout(timeout) {
            client = clientFromTargetId(deployment.targetId)
            val managementApi = ManagementClient.newInstance(ufUrl)

            deployment.deploymentInfo.forEach { deploymentInfo ->

                var actionStatus = managementApi.getTargetActionStatusAsync(basic, deployment.targetId, deploymentInfo.actionId).await()

                Assert.assertEquals(actionStatus, deploymentInfo.actionStatusOnStart)

                queue.add {
                    launch {
                        while(managementApi.getActionAsync(basic, deployment.targetId, deploymentInfo.actionId).await()
                                .status != Action.Status.finished) {
                            delay(100)
                        }
                        actionStatus = managementApi.getTargetActionStatusAsync(basic, deployment.targetId, deploymentInfo.actionId).await()

                        Assert.assertEquals(actionStatus.content, deploymentInfo.actionStatusOnFinish.content)

                        deploymentInfo.filesDownloadedPairedWithServerFile.forEach { (fileDownloaded, serverFile) ->
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
                while (queue.isNotEmpty()) {
                    delay(500) }
            }
        }
    }
}
