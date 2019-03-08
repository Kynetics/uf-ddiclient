/*
 * Copyright © 2017-2019 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.kynetics.updatefactory.ddiclient.api.model.response;

/**
 * Download information for all artifacts related to a specific {@link DdiChunk}
 *
 * @author Daniele Sergio
 */
public class DdiArtifact extends ResourceSupport {

    private String filename;

    private DdiArtifactHash hashes;

    private Long size;

    public DdiArtifactHash getHashes() {
        return hashes;
    }

    public void setHashes(final DdiArtifactHash hashes) {
        this.hashes = hashes;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(final String fileName) {
        filename = fileName;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(final Long size) {
        this.size = size;
    }

}
