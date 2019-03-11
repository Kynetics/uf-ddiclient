package com.kynetics.updatefactory.ddiclient.core.api

interface ConfigDataProvider {
    fun configData():Map<String, String> = DEFAULT_INSTACE.configData()

    companion object {
        val DEFAULT_INSTACE = object : ConfigDataProvider {
            override fun configData(): Map<String, String> = emptyMap()
        }
    }
}