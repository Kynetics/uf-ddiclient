package com.kynetics.updatefactory.ddiclient.core.api

import java.nio.file.Path

interface ConfigDataProvider {

    fun directoryPathForArtifacts(actionId:String): Path

    fun configData():Map<String, String> = emptyMap()

}