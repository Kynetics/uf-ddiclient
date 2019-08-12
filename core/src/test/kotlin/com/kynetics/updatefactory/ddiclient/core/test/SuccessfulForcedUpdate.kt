package com.kynetics.updatefactory.ddiclient.core.test

import com.kynetics.updatefactory.ddiclient.core.test.TestUtils.defaultActionStatusOnStart
import com.kynetics.updatefactory.ddiclient.core.test.TestUtils.endMessagesOnSuccessUpdate
import com.kynetics.updatefactory.ddiclient.core.test.TestUtils.filesDownloadedInOsWithAppsPairedToServerFile
import com.kynetics.updatefactory.ddiclient.core.test.TestUtils.firstActionEntry
import com.kynetics.updatefactory.ddiclient.core.test.TestUtils.locationOfFileNamed
import com.kynetics.updatefactory.ddiclient.core.test.TestUtils.messagesOnSuccefullyDownloadAppDistribution
import com.kynetics.updatefactory.ddiclient.core.test.TestUtils.messagesOnSuccefullyDownloadOsDistribution
import com.kynetics.updatefactory.ddiclient.core.test.TestUtils.messagesOnSuccessfullyDownloadOsWithAppDistribution
import com.kynetics.updatefactory.ddiclient.core.test.TestUtils.pathResolver
import com.kynetics.updatefactory.ddiclient.core.test.TestUtils.startMessagesOnUpdateFond
import com.kynetics.updatefactory.ddiclient.core.test.TestUtils.test1Artifact
import com.kynetics.updatefactory.ddiclient.core.test.TestUtils.test4Artifact
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

class SuccessfulForcedUpdate : AbstractClientTest() {

    @DataProvider(name = "targetUpdateProvider")
    fun dataProvider(): Array<TestUtils.TargetDeployments> {
        return arrayOf(target1AcceptFirstCancelRequestThenApplyAppUpdate(),
                target2ApplyOsUpdate(),
                target3ApplyOsWithAppsUpdate())
    }

    @Test(enabled = true, dataProvider = "targetUpdateProvider")
    fun test(targetDeployments: TestUtils.TargetDeployments) {
        testTemplate(targetDeployments)
    }

    private fun target1AcceptFirstCancelRequestThenApplyAppUpdate(): TestUtils.TargetDeployments {
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

        val filesDownloadedPairedToServerFile = setOf(pathResolver.fromArtifact("2").invoke(test1Artifact) to locationOfFileNamed("test1"))

        return TestUtils.TargetDeployments(
                targetId = targetId,
                targetToken = "4a28d893bb841def706073c789c0f3a7",
                deploymentInfo = listOf(
                        TestUtils.TargetDeployments.DeploymentInfo(
                                actionId = 1,
                                actionStatusOnStart = actionStatusOnStart1,
                                actionStatusOnFinish = contentEntriesOnFinish1,
                                filesDownloadedPairedWithServerFile = emptySet()
                        ),
                        TestUtils.TargetDeployments.DeploymentInfo(
                                actionId = 2,
                                actionStatusOnStart = defaultActionStatusOnStart,
                                actionStatusOnFinish = contentEntriesOnFinish2,
                                filesDownloadedPairedWithServerFile = filesDownloadedPairedToServerFile
                        )
                )
        )
    }

    private fun target2ApplyOsUpdate(): TestUtils.TargetDeployments {
        val targetId = "target2"

        val contentEntriesOnFinish = ActionStatus(setOf(*endMessagesOnSuccessUpdate,
                *messagesOnSuccefullyDownloadOsDistribution(targetId),
                *startMessagesOnUpdateFond))

        val filesDownloadedPairedToServerFile = setOf(pathResolver.fromArtifact("3").invoke(test4Artifact) to locationOfFileNamed("test4"))

        return TestUtils.TargetDeployments(
                targetId = targetId,
                targetToken = "0fe7b8c9de2102ec6bf305b6f66df5b2",
                deploymentInfo = listOf(
                        TestUtils.TargetDeployments.DeploymentInfo(
                                actionId = 3,
                                actionStatusOnStart = defaultActionStatusOnStart,
                                actionStatusOnFinish = contentEntriesOnFinish,
                                filesDownloadedPairedWithServerFile = filesDownloadedPairedToServerFile

                        )
                )
        )
    }

    private fun target3ApplyOsWithAppsUpdate(): TestUtils.TargetDeployments {
        val targetId = "target3"
        val actionId = 4
        val contentEntriesOnFinish = ActionStatus(setOf(
                *endMessagesOnSuccessUpdate,
                *messagesOnSuccessfullyDownloadOsWithAppDistribution(targetId),
                *startMessagesOnUpdateFond))
        return TestUtils.TargetDeployments(
                targetId = targetId,
                targetToken = "4a28d893bb841def706073c789c0f3a7",
                deploymentInfo = listOf(
                        TestUtils.TargetDeployments.DeploymentInfo(
                                actionId = actionId,
                                actionStatusOnStart = defaultActionStatusOnStart,
                                actionStatusOnFinish = contentEntriesOnFinish,
                                filesDownloadedPairedWithServerFile = filesDownloadedInOsWithAppsPairedToServerFile(actionId)

                        )
                )
        )
    }
}
