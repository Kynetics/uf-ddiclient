/*
 * Copyright © 2017-2018 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.kynetics.updatefactory.ddiclient.api.model.response;

import java.util.Collections;
import java.util.List;

/**
 * Deployment chunks.
 *
 * @author Daniele Sergio
 */
public class DdiChunk {

    private String part;

    private String version;

    private String name;

    private List<DdiArtifact> artifacts;

    public String getPart() {
        return part;
    }

    public String getVersion() {
        return version;
    }

    public String getName() {
        return name;
    }

    public List<DdiArtifact> getArtifacts() {
        if (artifacts == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(artifacts);
    }

}
