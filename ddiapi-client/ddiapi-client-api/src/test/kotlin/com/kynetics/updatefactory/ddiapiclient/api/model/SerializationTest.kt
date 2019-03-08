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

import com.google.gson.Gson
import org.testng.Assert.*
import org.testng.annotations.DataProvider
import org.testng.annotations.Test


class SerializationTest{


    private val gson = Gson()


    @DataProvider(name = "Serialization")
    fun objectsToSerialize(): Array<Any> {
        val ddiStatus = DdiStatus(ExecutionStatus.CANCELED,
                DdiResult(FinalResult.FAILURE,
                        DdiProgress(2,3)),
                listOf("deltail SerializationTest"))
        val ddiActionFeedback = DdiActionFeedback(1,
                "20140511T121314", ddiStatus)
        val ddiConfigData = DdiConfigData(1, "", ddiStatus, mapOf("key" to "value"))

        val ddiArtifactList = listOf(DdiArtifact("file.txt", DdiArtifactHash("sha1", "md5"), 10))

        val ddiCancel = DdiCancel("1", DdiCancelActionToStop("2"))

        val ddiControllerBase = DdiControllerBase(DdiConfig(DdiPolling("10:00")), emptyMap())

        val ddiDeploymentBase = DdiDeploymentBase("1",
                DdiDeployment(DdiDeployment.HandlingType.ATTEMPT,
                        DdiDeployment.HandlingType.FORCED,
                        listOf(DdiChunk("part", "version", "name",
                                ddiArtifactList))),
                DdiActionHistory("actionStatus", listOf("messages")),
                mapOf(LinkType.DEPLOYMENT to LinkEntry("https://ddi-api.host.com/TENANT_ID/controller/v1/CONTROLLER_ID/deploymentBase/81?c=-2056995960")))

        val error = Error("errorCode", "exceptionClass", 1, "message", listOf("details"))

        return arrayOf(ddiActionFeedback,
                ddiConfigData,
                ddiCancel,
                ddiControllerBase,
                ddiDeploymentBase,
                error)

    }

    @DataProvider(name = "JsonDeserialization")
    fun jsonToDeserialize(): Array<Pair<String, Class<*>>>{
        val polling = """{
    "config": {
        "polling": {
            "sleep": "00:05:00"
        }
    },
    "_links": {
        "cancelAction": {
            "href": "http://localhost:8081/default/controller/v1/target1/cancelAction/1"
        },
        "configData": {
            "href": "http://localhost:8081/default/controller/v1/target1/configData"
        }
    }
}"""
        return arrayOf(polling to DdiControllerBase::class.java)
    }

    @Test(dataProvider = "Serialization")
    fun serializationDeserialization(objectToTest: Any) {
        assertEquals(objectToTest,
                gson.fromJson(gson.toJson(objectToTest),
                        objectToTest.javaClass))
    }

    @Test(dataProvider = "JsonDeserialization")
    fun jsonDeseralization(json_objectTypePair: Pair<String, Class<*>>) {
        val objectDeserialized = gson.fromJson(json_objectTypePair.first, json_objectTypePair.second)
        assertNotNull(objectDeserialized)
        assertEquals(gson.toJson(objectDeserialized), json_objectTypePair.first )
    }
}
