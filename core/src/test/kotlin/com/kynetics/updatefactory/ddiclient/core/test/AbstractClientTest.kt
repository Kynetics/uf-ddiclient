package com.kynetics.updatefactory.ddiclient.core.test

import com.kynetics.updatefactory.ddiclient.core.UpdateFactoryClientDefaultImpl
import com.kynetics.updatefactory.ddiclient.core.api.*
import com.kynetics.updatefactory.ddiclient.core.test.TestUtils.basic
import com.kynetics.updatefactory.ddiclient.core.test.TestUtils.configDataProvider
import com.kynetics.updatefactory.ddiclient.core.test.TestUtils.defaultActionStatusOnStart
import com.kynetics.updatefactory.ddiclient.core.test.TestUtils.directoryDataProvider
import com.kynetics.updatefactory.ddiclient.core.test.TestUtils.downloadRootDirPath
import com.kynetics.updatefactory.ddiclient.core.test.TestUtils.endMessagesOnSuccessUpdate
import com.kynetics.updatefactory.ddiclient.core.test.TestUtils.filesDownloadedInOsWithAppsPairedToServerFile
import com.kynetics.updatefactory.ddiclient.core.test.TestUtils.firstActionEntry
import com.kynetics.updatefactory.ddiclient.core.test.TestUtils.gatewayToken
import com.kynetics.updatefactory.ddiclient.core.test.TestUtils.getDownloadDirectoryFromActionId
import com.kynetics.updatefactory.ddiclient.core.test.TestUtils.locationOfFileNamed
import com.kynetics.updatefactory.ddiclient.core.test.TestUtils.md5OfFileNamed
import com.kynetics.updatefactory.ddiclient.core.test.TestUtils.messagesOnSuccefullyDownloadAppDistribution
import com.kynetics.updatefactory.ddiclient.core.test.TestUtils.messagesOnSuccefullyDownloadOsDistribution
import com.kynetics.updatefactory.ddiclient.core.test.TestUtils.messagesOnSuccessfullyDownloadOsWithAppDistribution
import com.kynetics.updatefactory.ddiclient.core.test.TestUtils.startMessagesOnUpdateFond
import com.kynetics.updatefactory.ddiclient.core.test.TestUtils.tenantName
import com.kynetics.updatefactory.ddiclient.core.test.TestUtils.ufUrl
import com.kynetics.updatefactory.ddiclient.core.test.TestUtils.updater
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Credentials
import org.testng.Assert
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.io.File

abstract class AbstractClientTest{

    protected  var client: UpdateFactoryClient? = null

    open protected fun defaultClientFromTargetId(
            directoryDataProvider:DirectoryForArtifactsProvider = TestUtils.directoryDataProvider,
            configDataProvider: ConfigDataProvider = TestUtils.configDataProvider,
            updater: Updater = TestUtils.updater,
            deploymentPermitProvider: DeploymentPermitProvider = object : DeploymentPermitProvider{}): (String) -> UpdateFactoryClient = { targetId ->
        val clientData= UpdateFactoryClientData(
                tenantName,
                targetId,
                ufUrl,
                UpdateFactoryClientData.ServerType.UPDATE_FACTORY,
                gatewayToken )

        val client = UpdateFactoryClientDefaultImpl()
        client.init(
                clientData,
                directoryDataProvider,
                configDataProvider,
                deploymentPermitProvider,
                updater
        )
        client
    }


    protected fun testTemplate(deployment: TestUtils.TargetDeployments, clientFromTargetId: (String) -> UpdateFactoryClient = defaultClientFromTargetId())  = runBlocking{

        client = clientFromTargetId(deployment.targetId)
        val managementApi = ManagementClient.newInstance(ufUrl)

        deployment.deploymentInfo.forEach{ deploymentInfo ->

            var actionStatus = managementApi.getTargetActionStatusAsync(basic, deployment.targetId, deploymentInfo.actionId).await()

            Assert.assertEquals(actionStatus, deploymentInfo.actionStatusOnStart)

            client?.startAsync()

            launch {
                delay(4000) //todo replace with a client state listener
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

}
