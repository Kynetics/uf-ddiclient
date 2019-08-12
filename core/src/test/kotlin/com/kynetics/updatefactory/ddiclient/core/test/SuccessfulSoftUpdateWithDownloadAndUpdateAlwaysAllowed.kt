package com.kynetics.updatefactory.ddiclient.core.test

import com.kynetics.updatefactory.ddiclient.core.test.TestUtils.defaultActionStatusOnStart
import com.kynetics.updatefactory.ddiclient.core.test.TestUtils.endMessagesOnSuccessUpdate
import com.kynetics.updatefactory.ddiclient.core.test.TestUtils.filesDownloadedInOsWithAppsPairedToServerFile
import com.kynetics.updatefactory.ddiclient.core.test.TestUtils.messagesOnSuccessfullyDownloadOsWithAppDistribution
import com.kynetics.updatefactory.ddiclient.core.test.TestUtils.startMessagesOnUpdateFond
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

class SuccessfulSoftUpdateWithDownloadAndUpdateAlwaysAllowed : AbstractClientTest() {

    @DataProvider(name = "targetUpdateProvider")
    fun dataProvider(): Array<TestUtils.TargetDeployments> {
        return arrayOf(target4ApplyOsWithAppsUpdate())
    }

    @Test(enabled = true, dataProvider = "targetUpdateProvider")
    fun test(targetDeployments: TestUtils.TargetDeployments) {
        testTemplate(targetDeployments)
    }

    private fun target4ApplyOsWithAppsUpdate(): TestUtils.TargetDeployments {
        val targetId = "Target4"
        val actionId = 5
        val contentEntriesOnFinish = ActionStatus(setOf(
                *endMessagesOnSuccessUpdate,
                *messagesOnSuccessfullyDownloadOsWithAppDistribution(targetId),
                *startMessagesOnUpdateFond))
        return TestUtils.TargetDeployments(
                targetId = targetId,
                targetToken = "",
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
