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
        val tenantNameToLower = tenantName.toLowerCase().capitalize()
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

        fun messagesOnSuccessfullyDownloadOsWithAppDistribution(target:String) = arrayOf(
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.running, listOf("successfully downloaded all files")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.running, listOf("successfully downloaded file with md5 ${md5OfFileNamed("test1")}")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.running, listOf("successfully downloaded file with md5 ${md5OfFileNamed("test2")}")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.running, listOf("successfully downloaded file with md5 ${md5OfFileNamed("test3")}")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.running, listOf("successfully downloaded file with md5 ${md5OfFileNamed("test4")}")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.download, listOf("Update Server: Target downloads /$tenantNameToLower/controller/v1/$target/softwaremodules/2/artifacts/test_2")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.download, listOf("Update Server: Target downloads /$tenantNameToLower/controller/v1/$target/softwaremodules/2/artifacts/test_3")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.download, listOf("Update Server: Target downloads /$tenantNameToLower/controller/v1/$target/softwaremodules/1/artifacts/test_1")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.download, listOf("Update Server: Target downloads /$tenantNameToLower/controller/v1/$target/softwaremodules/3/artifacts/test_4")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.running, listOf("Start downloading 4 files"))
        )

        fun messagesOnSuccefullyDownloadOsDistribution(target:String) = arrayOf(
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.running, listOf("successfully downloaded all files")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.running, listOf("successfully downloaded file with md5 ${md5OfFileNamed("test4")}")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.download, listOf("Update Server: Target downloads /$tenantNameToLower/controller/v1/$target/softwaremodules/3/artifacts/test_4")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.running, listOf("Start downloading 1 files"))
        )

        fun messagesOnSuccefullyDownloadAppDistribution(target:String) = arrayOf(
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.running, listOf("successfully downloaded all files")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.running, listOf("successfully downloaded file with md5 ${md5OfFileNamed("test1")}")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.download, listOf("Update Server: Target downloads /$tenantNameToLower/controller/v1/$target/softwaremodules/1/artifacts/test_1")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.running, listOf("Start downloading 1 files")))

        val endMessagesOnSuccessUpdate = arrayOf(
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.finished, listOf("Update finished")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.running, listOf("Update applied")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.running, listOf("Applying the update..."))
        )

        val firstActionEntry = ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.running, listOf(null))

        val startMessagesOnUpdateFond = arrayOf(
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.retrieved, listOf("Update Server: Target retrieved update action and should start now the download.")),
                firstActionEntry)

        fun filesDownloadedInOsWithAppsPairedToServerFile(action:Int) = setOf("$downloadRootDirPath/$action/${md5OfFileNamed("test1")}" to locationOfFileNamed("test1"),
                "$downloadRootDirPath/$action/${md5OfFileNamed("test2")}" to locationOfFileNamed("test2"),
                "$downloadRootDirPath/$action/${md5OfFileNamed("test3")}" to locationOfFileNamed("test3"),
                "$downloadRootDirPath/$action/${md5OfFileNamed("test4")}" to locationOfFileNamed("test4"))

        val defaultActionStatusOnStart = ActionStatus(setOf(firstActionEntry))

    }

    @DataProvider(name = "targetUpdateProvider")
    fun dataProvider():Array<TargetDeployments>{
        return arrayOf(target1AcceptFirstCancelRequestThenApplyAppUpdate(),
                target2ApplyOsUpdate(),
                target3ApplyOsWithAppsUpdate(),
                target4ApplyOsWithAppsUpdate())
    }

    @Test(enabled = true, dataProvider = "targetUpdateProvider")
    fun testUpdate(deployment: TargetDeployments)  = runBlocking{

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

        deployment.deploymentInfo.forEach{ deploymentInfo ->

            var actionStatus = managementApi.getTargetActionStatusAsync(basic, deployment.targetId, deploymentInfo.actionId).await()

            Assert.assertEquals(actionStatus, deploymentInfo.actionStatusOnStart)

            client.startAsync()

            launch {
                delay(4000) //todo replace with a client state listener
                actionStatus = managementApi.getTargetActionStatusAsync(basic,deployment.targetId,deploymentInfo.actionId).await()
                println("receive:")
                println(actionStatus.content)
                println("expected:")
                println(deploymentInfo.actionStatusOnFinish.content)

                actionStatus.content.contains(deploymentInfo.actionStatusOnFinish.content.first())

                println("check1: ${actionStatus.content.size}")
                actionStatus.content.filter { !deploymentInfo.actionStatusOnFinish.content.contains(it)}
                        .forEach{println("$it")}

                println("check2: ${deploymentInfo.actionStatusOnFinish.content.size}")
                deploymentInfo.actionStatusOnFinish.content.filter { !actionStatus.content.contains(it) }
                        .forEach{println("$it")}

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

    private fun target1AcceptFirstCancelRequestThenApplyAppUpdate(): TargetDeployments {
        val targetId = "target1"
        val contentEntriesOnFinish2 = ActionStatus(setOf(*endMessagesOnSuccessUpdate,
                *messagesOnSuccefullyDownloadAppDistribution(targetId),
                *startMessagesOnUpdateFond))

        val actionStatusOnStart1 = ActionStatus(setOf(ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.canceling, listOf("Update Server: cancel obsolete action due to new update")),
                firstActionEntry))

        val contentEntriesOnFinish1 = ActionStatus(setOf(ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.canceled, listOf("Update Server: Cancelation confirmed.", "Update Server: Cancellation completion is finished sucessfully.")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.retrieved, listOf("Update Server: Target retrieved cancel action and should start now the cancelation.")),
                ActionStatus.ContentEntry(ActionStatus.ContentEntry.Type.canceling, listOf("Update Server: cancel obsolete action due to new update")),
                firstActionEntry))

        val filesDownloadedPairedToServerFile = setOf("$downloadRootDirPath/2/${md5OfFileNamed("test1")}" to locationOfFileNamed("test1"))

        return TargetDeployments(
                targetId = targetId,
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
                                actionStatusOnStart = defaultActionStatusOnStart,
                                actionStatusOnFinish = contentEntriesOnFinish2,
                                filesDownloadedPairedWithServerFile = filesDownloadedPairedToServerFile
                        )
                )
        )
    }

    private fun target2ApplyOsUpdate(): TargetDeployments {
        val targetId = "target2"

        val contentEntriesOnFinish = ActionStatus(setOf(*endMessagesOnSuccessUpdate,
                *messagesOnSuccefullyDownloadOsDistribution(targetId),
                *startMessagesOnUpdateFond))

        val filesDownloadedPairedToServerFile = setOf("$downloadRootDirPath/3/${md5OfFileNamed("test4")}" to locationOfFileNamed("test4"))

        return TargetDeployments(
                targetId = targetId,
                targetToken = "0fe7b8c9de2102ec6bf305b6f66df5b2",
                deploymentInfo = listOf(
                        TargetDeployments.DeploymentInfo(
                                actionId = 3,
                                actionStatusOnStart = defaultActionStatusOnStart,
                                actionStatusOnFinish = contentEntriesOnFinish,
                                filesDownloadedPairedWithServerFile = filesDownloadedPairedToServerFile

                        )
                )
        )
    }

    private fun target3ApplyOsWithAppsUpdate(): TargetDeployments {
        val targetId = "target3"
        val actionId = 4
        val contentEntriesOnFinish = ActionStatus(setOf(
                *endMessagesOnSuccessUpdate,
                *messagesOnSuccessfullyDownloadOsWithAppDistribution(targetId),
                *startMessagesOnUpdateFond))
        return TargetDeployments(
                targetId = targetId,
                targetToken = "4a28d893bb841def706073c789c0f3a7",
                deploymentInfo = listOf(
                        TargetDeployments.DeploymentInfo(
                                actionId = actionId,
                                actionStatusOnStart = defaultActionStatusOnStart,
                                actionStatusOnFinish = contentEntriesOnFinish,
                                filesDownloadedPairedWithServerFile = filesDownloadedInOsWithAppsPairedToServerFile(actionId)

                        )
                )
        )
    }

    private fun target4ApplyOsWithAppsUpdate(): TargetDeployments {
        val targetId = "Target4"
        val actionId = 5
        val contentEntriesOnFinish = ActionStatus(setOf(
                *endMessagesOnSuccessUpdate,
                *messagesOnSuccessfullyDownloadOsWithAppDistribution(targetId),
                *startMessagesOnUpdateFond))
        return TargetDeployments(
                targetId = targetId,
                targetToken = "",
                deploymentInfo = listOf(
                        TargetDeployments.DeploymentInfo(
                                actionId = actionId,
                                actionStatusOnStart = defaultActionStatusOnStart,
                                actionStatusOnFinish = contentEntriesOnFinish,
                                filesDownloadedPairedWithServerFile = filesDownloadedInOsWithAppsPairedToServerFile(actionId)

                        )
                )
        )
    }

}
