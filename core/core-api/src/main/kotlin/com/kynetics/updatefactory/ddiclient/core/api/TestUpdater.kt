package com.kynetics.updatefactory.ddiclient.core.api

import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.nio.file.Paths

class TestUpdater(private val path: String) : ConfigDataProvider, Updater {

    override fun directoryPathForArtifacts(actionId: String): Path {
        return Paths.get(path,actionId)
    }

    override fun apply(modules: Set<Updater.SwModuleWithPath>) {
        modules.forEach {
            LOG.debug("apply module ${it.name} ${it.version} of type ${it.type}")
            it.artifacts.forEach { a ->
                LOG.debug("install artifact ${a.filename} from file ${a.path}")
            }
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(TestUpdater::class.java)
    }
}