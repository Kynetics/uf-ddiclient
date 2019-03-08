/*
 * Copyright © 2017-2019 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.updatefactory.ddiapiclient.api.model

import org.testng.Assert
import org.testng.Assert.assertNull
import org.testng.annotations.DataProvider
import org.testng.annotations.Test


class GetActionIDTest{


    companion object {
        private const val deploymentId = 88L
        private const val cancelId = 77L
        private val deploymentEntry = LinkEntry("https://ddi-api.host.com/TENANT_ID/controller/v1/CONTROLLER_ID/deploymentBase/$deploymentId?c=-2056995960")
        private val cancelEntry = LinkEntry("https://ddi-api.host.com/TENANT_ID/controller/v1/CONTROLLER_ID/cancelAction/$cancelId")
        private val configDataUrl = LinkEntry("https://ddi-api.host.com/TENANT_ID/controller/v1/CONTROLLER_ID/configData")
    }

    @DataProvider(name = "idNotFoundDataProvider")
    fun idNotFoundDataProvider(): Array<Pair<LinkType, LinkEntry>> {

        return arrayOf(
                LinkType.DEPLOYMENT to configDataUrl,
                LinkType.DEPLOYMENT to cancelEntry,
                LinkType.CANCEL to configDataUrl,
                LinkType.CANCEL to deploymentEntry,
                LinkType.CONFIG_DATA to configDataUrl,
                LinkType.CONFIG_DATA to cancelEntry,
                LinkType.CONFIG_DATA to deploymentEntry)

    }

    @DataProvider(name = "idFoundDataProvider")
    fun idFoundDataProvider(): Array<Triple<LinkType, LinkEntry, Long>> {
        return arrayOf(
                Triple(LinkType.DEPLOYMENT, deploymentEntry, deploymentId),
                Triple(LinkType.CANCEL, cancelEntry, cancelId))
    }


    @Test(dataProvider = "idNotFoundDataProvider")
    fun testIdNotFound( pair: Pair<LinkType, LinkEntry>) {
        assertNull(pair.first.getActionId(pair.second))
    }

    @Test(dataProvider = "idFoundDataProvider")
    fun testIdFound(triple: Triple<LinkType, LinkEntry, Int>) {
        Assert.assertEquals(triple.first.getActionId(triple.second), triple.third)
    }
}
