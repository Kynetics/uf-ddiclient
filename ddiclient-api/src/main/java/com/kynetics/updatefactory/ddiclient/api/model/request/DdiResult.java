/*
 * Copyright © 2017 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.kynetics.updatefactory.ddiclient.api.model.request;

import com.google.gson.annotations.SerializedName;

/**
 * Result information of the action progress which can by an intermediate or
 * final update.
 *
 * @author Daniele Sergio
 */
public class DdiResult {

    private final FinalResult finished;

    private final DdiProgress progress;


    public DdiResult(FinalResult finished, DdiProgress progress) {
        this.finished = finished;
        this.progress = progress;
    }

    public FinalResult getFinished() {
        return finished;
    }

    public DdiProgress getProgress() {
        return progress;
    }

    /**
     * Defined status of the final result.
     *
     */
    public enum FinalResult {
        /**
         * Execution was successful.
         */
        @SerializedName("success")
        SUCESS("success"),

        /**
         * Execution terminated with errors or without the expected result.
         */
        @SerializedName("failure")
        FAILURE("failure"),

        /**
         * No final result could be determined (yet).
         */
        @SerializedName("none")
        NONE("none");

        private String name;

        FinalResult(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    @Override
    public String toString() {
        return "Result [finished=" + finished + ", progress=" + progress + "]";
    }

}
