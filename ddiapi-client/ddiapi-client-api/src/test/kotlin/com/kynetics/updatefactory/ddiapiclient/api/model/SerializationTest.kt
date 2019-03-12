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
import org.testng.Assert.assertEquals
import org.testng.annotations.DataProvider
import org.testng.annotations.Test


class SerializationTest{


    val gson = Gson()


    @DataProvider(name = "Serialization")
    fun objectsToSerialize(): Array<Any> {
        val cfgDataReq = CfgDataReq.of(emptyMap(), CfgDataReq.Mod.merge)
        return arrayOf(cfgDataReq.copy(data = mapOf("ciao" to "miao")))

    }

    @Test(dataProvider = "Serialization")
    fun serialization(expected: Any) {
        val json = gson.toJson(expected)
        println(json)
        val actual = gson.fromJson(json, expected.javaClass)
        assertEquals(actual,expected)
    }
}
