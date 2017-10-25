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

import java.util.Collections;
import java.util.List;

/**
 * Details status information concerning the action processing.
 *
 * @author Daniele Sergio
 */
public class DdiStatus {

    private final ExecutionStatus execution;

    private final DdiResult result;

    private final List<String> details;

    public DdiStatus(ExecutionStatus execution, DdiResult result, List<String> details) {
        this.execution = execution;
        this.result = result;
        this.details = details;
    }

    public ExecutionStatus getExecution() {
        return execution;
    }

    public DdiResult getResult() {
        return result;
    }

    public List<String> getDetails() {
        if (details == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(details);
    }

    /**
     * The element status contains information about the execution of the
     * operation.
     *
     */
    public enum ExecutionStatus {
        /**
         * Execution of the action has finished.
         */
        @SerializedName("closed")
        CLOSED("closed"),

        /**
         * Execution has started but has not yet finished.
         */
        @SerializedName("proceeding")
        PROCEEDING("proceeding"),

        /**
         * Execution was suspended from outside.
         */
        @SerializedName("canceled")
        CANCELED("canceled"),

        /**
         * Action has been noticed and is intended to run.
         */
        @SerializedName("scheduled")
        SCHEDULED("scheduled"),

        /**
         * Action was not accepted.
         */
        @SerializedName("rejected")
        REJECTED("rejected"),

        /**
         * Action is started after a reset, power loss, etc.
         */
        @SerializedName("resumed")
        RESUMED("resumed");

        private String name;

        ExecutionStatus(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    @Override
    public String toString() {
        return "Status [execution=" + execution + ", result=" + result + ", details=" + details + "]";
    }

}
