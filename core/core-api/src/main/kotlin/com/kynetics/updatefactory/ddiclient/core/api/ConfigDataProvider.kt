package com.kynetics.updatefactory.ddiclient.core.api

interface ConfigDataProvider {

    fun configData():Map<String, String> = emptyMap()

}