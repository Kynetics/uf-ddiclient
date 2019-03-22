package com.kynetics.updatefactory.ddiclient.core.test

import com.kynetics.updatefactory.ddiclient.core.UpdateFactoryClientDefaultImpl
import com.kynetics.updatefactory.ddiclient.core.api.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Credentials
import org.testng.Assert
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.io.File

class ClientTest{

    data class TargetDeployments(
            val targetId: String,
            val targetToken: String,
            val deploymentInfo: List<DeploymentInfo>
    ){
        data class DeploymentInfo(
                val actionId: Int,
                val actionStatusOnStart: ActionStatus,
                val actionStatusOnFinish: ActionStatus,
                val filesDownloadedPairedWithServerFile: Set<Pair<String,String>>
        )
    }

    companion object {
        val tenantName = "DEFAULT"
        val basic = Credentials.basic("$tenantName\\test", "test")
        val ufUrl = "http://localhost:8081"
        val downloadRootDirPath = "./build/test/download/"
        val gatewayToken = "66076ab945a127dd80b15e9011995109"
        val getDownloadDirectoryFromActionId = { actionId:String -> File("$downloadRootDirPath/$actionId") }
        val directoryDataProvider = object : DirectoryForArtifactsProvider { override fun directoryForArtifacts(actionId: String): File = getDownloadDirectoryFromActionId.invoke(actionId) }
        val configDataProvider = object : ConfigDataProvider {}
        val updater = object : Updater {
            override fun apply(modules: Set<Updater.SwModuleWithPath>, messanger: Updater.Messanger): Boolean {
                println("APPLY UPDATE $modules")
                messanger.sendMessageToServer("Applying the update...")
                messanger.sendMessageToServer("Update applied")
                return true
            }
        }

        val serverFilesMappedToLocantionAndMd5 = mapOf("test1" to Pair("../docker/test/artifactrepo/$tenantName/4b/5a/b54e43082887d1e7cdb10b7a21fe4a1e56b44b5a","2490a3d39b0004e4afeb517ef0ddbe2d") ,
                "test2" to Pair("../docker/test/artifactrepo/$tenantName/b6/1e/a096a9d3cb96fa4cf6c63bd736a84cb7a7e4b61e","b0b3b0dbf5330e3179c6ae3e0ac524c9"),
                "test3" to Pair("../docker/test/artifactrepo/$tenantName/bf/94/cde0c01b26634f869bb876326e4fbe969792bf94","2244fbd6bee5dcbe312e387c062ce6e6"),
                "test4" to Pair("../docker/test/artifactrepo/$tenantName/dd/0a/07fa4d03ac54d0b2a52f23d8e878c96db7aadd0a","94424c5ce3f8c57a5b26d02f37dc06fc"))

        val md5OfFileNamed: (String) -> String = {key -> serverFilesMappedToLocantionAndMd5.getValue(key).second}
        val locationOfFileNamed:  (String) -> String = {key -> serverFilesMappedToLocantionAndMd5.getValue(key).first}
    }


    @DataProvider(name = "TargetDeployments")
    fun dataProvider(): Array<TargetDeployments> {
        return arrayOf(targetDeploymentsForTarget1(), targetDeploymentsForTarget2(), targetDeploymentsForTarget3())

    }

    @Test(enabled = true, dataProvider = "TargetDeployments")
    fun test(deployment: TargetDeployments)  = runBlocking{

        val clientData= UpdateFactoryClientData(
                tenantName,
                deployment.targetId,
                ufUrl,
                UpdateFactoryClientData.ServerType.UPDATE_FACTORY,
                gatewayToken )

        val client = UpdateFactoryClientDefaultImpl()
        client.init(
                clientData,
                directoryDataProvider,
                configDataProvider,
                object : DeploymentPermitProvider {
                    override fun downloadAllowed(): Boolean = true
                    override fun updateAllowed(): Boolean = true
                },
                updater
        )

        val managementApi = ManagementClient.newInstance(ufUrl)

        deployment.deploymentInfo.forEach{

            var actionStatus = managementApi.getTargetActionStatusAsync(basic, deployment.targetId, it.actionId).await()

            Assert.assertEquals(actionStatus, it.actionStatusOnStart)

            client.startAsync()

            launch {
                delay(8000) //todo replace with a client state listener
                actionStatus = managementApi.getTargetActionStatusAsync(basic,deployment.targetId,it.actionId).await()
                Assert.assertEquals(actionStatus.content, it.actionStatusOnFinish.content)

                it.filesDownloadedPairedWithServerFile.forEach{
                    println(File(it.first).absolutePath)
                    println(File(it.second).absolutePath)
                    Assert.assertEquals(File(it.first).readText(), File(it.second).readText())
                }

                getDownloadDirectoryFromActionId(it.actionId.toString()).deleteRecursively()
            }

        }

    }

    private fun targetDeploymentsForTarget2(): TargetDeployments {
        val contentEntriesOnFinish = ActionStatus(setOf(ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.finished, listOf("Update finished")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.running, listOf("Update applied")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.running, listOf("Applying the update...")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.running, listOf("successfully downloaded all files")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.running, listOf("successfully downloaded file with md5 ${md5OfFileNamed("test4")}")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.download, listOf("Update Server: Target downloads /Default/controller/v1/target2/softwaremodules/3/artifacts/test_4")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.running, listOf("Start downloading 1 files")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.retrieved, listOf("Update Server: Target retrieved update action and should start now the download.")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.running, listOf(null))))

        val actionStatusOnStart = ActionStatus(setOf(ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.running, listOf(null))))

        val filesDownloadedPairedToServerFile = setOf("$downloadRootDirPath/3/${md5OfFileNamed("test4")}" to locationOfFileNamed("test4"))

        return TargetDeployments(
                targetId = "target2",
                targetToken = "0fe7b8c9de2102ec6bf305b6f66df5b2",
                deploymentInfo = listOf(
                        TargetDeployments.DeploymentInfo(
                                actionId = 3,
                                actionStatusOnStart = actionStatusOnStart,
                                actionStatusOnFinish = contentEntriesOnFinish,
                                filesDownloadedPairedWithServerFile = filesDownloadedPairedToServerFile

                        )
                )
        )
    }

    private fun targetDeploymentsForTarget3(): TargetDeployments {
        val contentEntriesOnFinish = ActionStatus(setOf(ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.finished, listOf("Update finished")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.running, listOf("Update applied")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.running, listOf("Applying the update...")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.running, listOf("successfully downloaded all files")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.running, listOf("successfully downloaded file with md5 ${md5OfFileNamed("test1")}")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.running, listOf("successfully downloaded file with md5 ${md5OfFileNamed("test2")}")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.running, listOf("successfully downloaded file with md5 ${md5OfFileNamed("test3")}")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.running, listOf("successfully downloaded file with md5 ${md5OfFileNamed("test4")}")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.download, listOf("Update Server: Target downloads /Default/controller/v1/target3/softwaremodules/2/artifacts/test_2")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.download, listOf("Update Server: Target downloads /Default/controller/v1/target3/softwaremodules/2/artifacts/test_3")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.download, listOf("Update Server: Target downloads /Default/controller/v1/target3/softwaremodules/1/artifacts/test_1")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.download, listOf("Update Server: Target downloads /Default/controller/v1/target3/softwaremodules/3/artifacts/test_4")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.running, listOf("Start downloading 4 files")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.retrieved, listOf("Update Server: Target retrieved update action and should start now the download.")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.running, listOf(null))))

        val actionStatusOnStart = ActionStatus(setOf(ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.running, listOf(null))))


        val filesDownloadedPairedToServerFile = setOf("$downloadRootDirPath/4/${md5OfFileNamed("test1")}" to locationOfFileNamed("test1"),
                "$downloadRootDirPath/4/${md5OfFileNamed("test2")}" to locationOfFileNamed("test2"),
                "$downloadRootDirPath/4/${md5OfFileNamed("test3")}" to locationOfFileNamed("test3"),
                "$downloadRootDirPath/4/${md5OfFileNamed("test4")}" to locationOfFileNamed("test4"))
        return TargetDeployments(
                targetId = "target3",
                targetToken = "4a28d893bb841def706073c789c0f3a7",
                deploymentInfo = listOf(
                        TargetDeployments.DeploymentInfo(
                                actionId = 4,
                                actionStatusOnStart = actionStatusOnStart,
                                actionStatusOnFinish = contentEntriesOnFinish,
                                filesDownloadedPairedWithServerFile = filesDownloadedPairedToServerFile

                        )
                )
        )
    }

    private fun targetDeploymentsForTarget1(): TargetDeployments {
        val contentEntriesOnFinish2 = ActionStatus(setOf(ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.finished, listOf("Update finished")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.running, listOf("Update applied")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.running, listOf("Applying the update...")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.running, listOf("successfully downloaded all files")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.running, listOf("successfully downloaded file with md5 ${md5OfFileNamed("test1")}")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.download, listOf("Update Server: Target downloads /Default/controller/v1/target1/softwaremodules/1/artifacts/test_1")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.running, listOf("Start downloading 1 files")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.retrieved, listOf("Update Server: Target retrieved update action and should start now the download.")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.running, listOf(null))))

        val actionStatusOnStart1 = ActionStatus(setOf( /*ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.retrieved, listOf("Update Server: Target retrieved cancel action and should start now the cancelation.")),*/
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.canceling, listOf("Update Server: cancel obsolete action due to new update")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.running, listOf(null))))

        val contentEntriesOnFinish1 = ActionStatus(setOf(ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.canceled, listOf("Update Server: Cancelation confirmed.", "Update Server: Cancellation completion is finished sucessfully.")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.retrieved, listOf("Update Server: Target retrieved cancel action and should start now the cancelation.")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.canceling, listOf("Update Server: cancel obsolete action due to new update")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.running, listOf(null))))

        val actionStatusOnStart2 = ActionStatus(setOf(ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.running, listOf(null))))

        val filesDownloadedPairedToServerFile = setOf("$downloadRootDirPath/2/${md5OfFileNamed("test1")}" to locationOfFileNamed("test1"))

        return TargetDeployments(
                targetId = "target1",
                targetToken = "4a28d893bb841def706073c789c0f3a7",
                deploymentInfo = listOf(
                        TargetDeployments.DeploymentInfo(
                                actionId = 1,
                                actionStatusOnStart = actionStatusOnStart1,
                                actionStatusOnFinish = contentEntriesOnFinish1,
                                filesDownloadedPairedWithServerFile = emptySet()
                        ),
                        TargetDeployments.DeploymentInfo(
                                actionId = 2,
                                actionStatusOnStart = actionStatusOnStart2,
                                actionStatusOnFinish = contentEntriesOnFinish2,
                                filesDownloadedPairedWithServerFile = filesDownloadedPairedToServerFile
                        )
                )
        )
    }
}
