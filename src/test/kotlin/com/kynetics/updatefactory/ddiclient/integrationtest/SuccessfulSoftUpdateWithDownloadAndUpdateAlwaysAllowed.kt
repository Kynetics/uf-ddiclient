/*
 * Copyright © 2017-2021  Kynetics  LLC
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package com.kynetics.updatefactory.ddiclient.integrationtest

import com.kynetics.updatefactory.ddiclient.integrationtest.TestUtils.defaultActionStatusOnStart
import com.kynetics.updatefactory.ddiclient.integrationtest.TestUtils.endMessagesOnSuccessUpdate
import com.kynetics.updatefactory.ddiclient.integrationtest.TestUtils.filesDownloadedInOsWithAppsPairedToServerFile
import com.kynetics.updatefactory.ddiclient.integrationtest.TestUtils.messagesOnSuccessfullyDownloadOsWithAppDistribution
import com.kynetics.updatefactory.ddiclient.integrationtest.TestUtils.startMessagesOnUpdateFond
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
        val contentEntriesOnFinish = ActionStatus(
            setOf(
                *endMessagesOnSuccessUpdate,
                *messagesOnSuccessfullyDownloadOsWithAppDistribution(targetId),
                *startMessagesOnUpdateFond
            )
        )
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
