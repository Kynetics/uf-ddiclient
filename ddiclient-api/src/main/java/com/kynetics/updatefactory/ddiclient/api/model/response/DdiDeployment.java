/*
 * Copyright © 2017 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.kynetics.updatefactory.ddiclient.api.model.response;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

/**
 * Detailed update action information.
 *
 * @author Daniele Sergio
 */
public class DdiDeployment {

    private HandlingType download;

    private HandlingType update;

    private List<DdiChunk> chunks;

    public HandlingType getDownload() {
        return download;
    }

    public HandlingType getUpdate() {
        return update;
    }

    public List<DdiChunk> getChunks() {
        if (chunks == null) {
            return Collections.emptyList();
        }
        
        return Collections.unmodifiableList(chunks);
    }

    /**
     * The handling type for the update action.
     */
    public enum HandlingType {

        /**
         * Not necessary for the command.
         */
        @SerializedName("skip")
        SKIP("skip"),

        /**
         * Try to execute (local applications may intervene by SP control API).
         */
        @SerializedName("attempt")
        ATTEMPT("attempt"),

        /**
         * Execution independent of local intervention attempts.
         */
        @SerializedName("forced")
        FORCED("forced");

        private String name;

        HandlingType(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    @Override
    public String toString() {
        return "Deployment [download=" + download + ", update=" + update + ", chunks=" + chunks + "]";
    }

}
