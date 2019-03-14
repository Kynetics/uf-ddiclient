package com.kynetics.updatefactory.ddiclient.core.api

import java.io.File

interface DirectoryForArtifactsProvider {

    fun directoryForArtifacts(actionId:String): File

}