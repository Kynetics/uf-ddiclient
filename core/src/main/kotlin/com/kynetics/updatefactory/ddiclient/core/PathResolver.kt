package com.kynetics.updatefactory.ddiclient.core

import com.kynetics.updatefactory.ddiclient.core.api.DirectoryForArtifactsProvider
import com.kynetics.updatefactory.ddiclient.core.api.Updater
import java.io.File

class PathResolver(private val dfap: DirectoryForArtifactsProvider) {

    companion object {
        const val ROOT = "artifacts"
    }

    fun fromArtifact(id: String):(artifact: Updater.SwModule.Artifact) -> String {
        return { artifact ->
            File(dfap.directoryForArtifacts(), "$ROOT/$id/${artifact.hashes.md5}").absolutePath
        }
    }

    fun baseDirectory():File{
        return File(dfap.directoryForArtifacts(), ROOT)
    }

    fun updateDir(actionId:String):File{
        return File(baseDirectory(), actionId)
    }
}