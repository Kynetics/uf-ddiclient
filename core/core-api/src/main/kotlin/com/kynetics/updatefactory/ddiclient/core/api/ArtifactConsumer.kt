package com.kynetics.updatefactory.ddiclient.core.api

interface ArtifactConsumer {

    fun selectArtifacts(softwareModules: Set<SwModule>):Set<Hashes> = DEFAULT_INSTACE.selectArtifacts(softwareModules)

    data class SwModule(
            val metadata: Set<Metadata>?,
            val type: String,
            val name: String,
            val version: String,
            val artifacts: Set<Artifact>){
        data class Metadata(
                val key: String,
                val value: String)
        data class Artifact(
                val filename: String,
                val hashes: Hashes,
                val size: Long)
    }

    data class Hashes(
            val sha1: String,
            val md5: String)

    companion object {
        val DEFAULT_INSTACE = object : ArtifactConsumer {
            override fun selectArtifacts(softwareModules: Set<SwModule>): Set<Hashes> =
                    softwareModules.flatMap { sm -> sm.artifacts.map { it.hashes } }.toSet()
        }
    }

}