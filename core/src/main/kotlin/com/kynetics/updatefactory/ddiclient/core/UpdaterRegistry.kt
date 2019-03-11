package com.kynetics.updatefactory.ddiclient.core

import com.kynetics.updatefactory.ddiapiclient.api.model.DeplBaseResp
import com.kynetics.updatefactory.ddiclient.core.api.ArtifactConsumer

class UpdaterRegistry {

    fun allRequiredArtifactsFor(chunks: Set<DeplBaseResp.Depl.Cnk>):Set<ArtifactConsumer.Hashes> =
            ArtifactConsumer.DEFAULT_INSTACE.selectArtifacts(chunks.map { convert(it) }.toSet())

    private fun convert(cnk:DeplBaseResp.Depl.Cnk):ArtifactConsumer.SwModule =
            ArtifactConsumer.SwModule(
                    cnk.metadata?.map { convert(it) }?.toSet(),
                    cnk.part,
                    cnk.name,
                    cnk.version,
                    cnk.artifacts.map { convert(it) }.toSet())

    private fun convert(mtdt: DeplBaseResp.Depl.Cnk.Mtdt): ArtifactConsumer.SwModule.Metadata =
            ArtifactConsumer.SwModule.Metadata(
                    mtdt.key,
                    mtdt.value)

    private fun convert(artfct: DeplBaseResp.Depl.Cnk.Artfct): ArtifactConsumer.SwModule.Artifact =
            ArtifactConsumer.SwModule.Artifact(
                    artfct.filename,
                    ArtifactConsumer.Hashes(
                            artfct.hashes.sha1,
                            artfct.hashes.md5),
                    artfct.size)
}