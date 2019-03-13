package com.kynetics.updatefactory.ddiclient.core

import com.kynetics.updatefactory.ddiapiclient.api.model.DeplBaseResp
import com.kynetics.updatefactory.ddiclient.core.api.Updater

class UpdaterRegistry(private vararg val updaters: Updater) {

    fun allRequiredArtifactsFor(chunks: Set<DeplBaseResp.Depl.Cnk>):Set<Updater.Hashes> =
            updaters.flatMap { u ->
                u.requiredSoftwareModulesAndPriority(chunks.map { convert(it) }.toSet())
                        .swModules.flatMap { it.hashes } }.toSet()

//    fun allRequiredSwModuleFor(chunks: Set<DeplBaseResp.Depl.Cnk>): Set<Updater.SwModsApplication> =
//            updaters.map { u -> u.requiredSoftwareModulesAndPriority(chunks.map { convert(it) }.toSet())}.

    private fun convert(cnk:DeplBaseResp.Depl.Cnk):Updater.SwModule =
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