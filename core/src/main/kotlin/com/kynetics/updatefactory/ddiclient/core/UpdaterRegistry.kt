package com.kynetics.updatefactory.ddiclient.core

import com.kynetics.updatefactory.ddiapiclient.api.model.DeplBaseResp
import com.kynetics.updatefactory.ddiclient.core.api.Updater

class UpdaterRegistry(private vararg val updaters: Updater) {

    fun allRequiredArtifactsFor(chunks: Set<DeplBaseResp.Depl.Cnk>): Set<Updater.Hashes> =
            updaters.flatMap { u ->
                u.requiredSoftwareModulesAndPriority(chunks.map { convert(it) }.toSet())
                        .swModules.flatMap { it.hashes } }.toSet()

    fun allUpdatersWithSwModulesOrderedForPriority(chunks: Set<DeplBaseResp.Depl.Cnk>): Set<UpdaterWithSwModule> {

        val swModules = chunks.map { convert(it) }.toSet()

        return updaters.map { u ->
            val appl = u.requiredSoftwareModulesAndPriority(swModules)
            UpdaterWithSwModule(appl.priority, u, appl.swModules.map { swm ->
                swModules.find {
                    with(swm) {
                        it.name == name &&
                                it.type == type &&
                                it.version == version
                    }
                }!!
            }.toSet())
        }.toSortedSet(Comparator { p1, p2 -> p1.priority.compareTo(p2.priority) })
    }

    fun currentUpdateIsCancellable(): Boolean {
        return updaters.map { it.updateIsCancellable() }
                .reduce { acc, value -> acc && value }
    }

    data class UpdaterWithSwModule(val priority: Int, val updater: Updater, val softwareModules: Set<Updater.SwModule>)

    private fun convert(cnk: DeplBaseResp.Depl.Cnk): Updater.SwModule =
            Updater.SwModule(
                    cnk.metadata?.map { convert(it) }?.toSet(),
                    cnk.part,
                    cnk.name,
                    cnk.version,
                    cnk.artifacts.map { convert(it) }.toSet())

    private fun convert(mtdt: DeplBaseResp.Depl.Cnk.Mtdt): Updater.SwModule.Metadata =
            Updater.SwModule.Metadata(
                    mtdt.key,
                    mtdt.value)

    private fun convert(artfct: DeplBaseResp.Depl.Cnk.Artfct): Updater.SwModule.Artifact =
            Updater.SwModule.Artifact(
                    artfct.filename,
                    Updater.Hashes(
                            artfct.hashes.sha1,
                            artfct.hashes.md5),
                    artfct.size)
}
