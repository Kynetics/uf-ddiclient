/*
 * Copyright © 2017-2021  Kynetics  LLC
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package com.kynetics.updatefactory.ddiclient.core

import com.kynetics.updatefactory.ddiclient.core.api.DirectoryForArtifactsProvider
import com.kynetics.updatefactory.ddiclient.core.api.Updater
import java.io.File

class PathResolver(private val dfap: DirectoryForArtifactsProvider) {

    companion object {
        const val ROOT = "artifacts"
    }

    fun fromArtifact(id: String): (artifact: Updater.SwModule.Artifact) -> String {
        return { artifact ->
            File(dfap.directoryForArtifacts(), "$ROOT/$id/${artifact.hashes.md5}").absolutePath
        }
    }

    fun baseDirectory(): File {
        return File(dfap.directoryForArtifacts(), ROOT)
    }

    fun updateDir(actionId: String): File {
        return File(baseDirectory(), actionId)
    }
}
