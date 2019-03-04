/*
 * Copyright © 2017-2019 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.updatefactory.ddiapiclient.model.request

import com.google.gson.annotations.SerializedName
import java.util.Collections.emptyList

data class DdiActionFeedback(val id: Long, val time: String, val status: DdiStatus)

data class DdiConfigData(val id: Long, val time: String, val status: DdiStatus, val data: Map<String, String>)

/**
 * Details status information concerning the action processing.
 *
 */
data class DdiStatus(val execution: ExecutionStatus, val result: DdiResult, private val details: List<String> = emptyList())

/**
 * Result information of the action progress which can by an intermediate or
 * final update.
 *
 */
data class DdiResult(val finished: FinalResult, val progress: DdiProgress)

data class DdiProgress(val cnt: Int, val of: Int)

/**
 * The element status contains information about the execution of the
 * operation.
 *
 */
enum class ExecutionStatus constructor(@SerializedName("name")val statusName: String) {
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
    RESUMED("resumed")
}

/**
 * Defined status of the final result.
 *
 */
enum class FinalResult constructor(val resultName: String) {
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
    NONE("none")
}