package com.kynetics.updatefactory.ddiclient.virtualdevice.entrypoint

import com.kynetics.updatefactory.ddiclient.core.api.DirectoryForArtifactsProvider
import com.kynetics.updatefactory.ddiclient.virtualdevice.Configuration
import java.io.File

class DirectoryForArtifactsProviderImpl(private val controllerId:String): DirectoryForArtifactsProvider {
    override fun directoryForArtifacts(): File = File("${Configuration.storagePath}/$controllerId")
}