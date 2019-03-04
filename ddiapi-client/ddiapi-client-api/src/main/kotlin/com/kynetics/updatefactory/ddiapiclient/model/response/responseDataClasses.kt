/*
 * Copyright © 2017-2019 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.updatefactory.ddiapiclient.model.response

import com.google.gson.annotations.SerializedName
import java.net.MalformedURLException
import java.net.URL
import java.util.*

data class DdiActionHistory(
        @SerializedName("status")val actionStatus: String? = null,
        @SerializedName("messages") val messages: List<String>? = null)

data class DdiArtifact(val fileName:String, val hashes:DdiArtifactHash, val size: Long) : ResourceSupport()

data class DdiArtifactHash(val sha1:String? = null, val md5:String? = null)

data class DdiCancel(val id: String, val cancelAction: DdiCancelActionToStop )

data class DdiCancelActionToStop(val stopId: String)

data class DdiChunk(val part: String, val version: String, val name: String, val artifacts: List<DdiArtifact>)

data class DdiConfig(val polling: DdiPolling)

data class DdiControllerBase(val config: DdiConfig) : ResourceSupport()

data class DdiDeployment(val download: HandlingType, val update: HandlingType, val chunks: List<DdiChunk>){

    /**
     * The handling type for the update action.
     */
    enum class HandlingType private constructor(val handlingTypeName: String) {

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
        FORCED("forced")
    }


}

data class DdiDeploymentBase(@SerializedName("id") private val deplyomentId: String,
                             @SerializedName("deployment") val deployment: DdiDeployment,
                             @SerializedName("actionHistory")val actionHistory: DdiActionHistory? = null) : ResourceSupport()

data class DdiPolling(val sleep: String)

data class Error (@SerializedName(value = "errorCode", alternate = ["status"]) val errorCode: String? = null,
                  @SerializedName(value = "exceptionClass", alternate = ["exception"]) val exceptionClass: String? = null,
                  var code: Int = 0,
                  val message: String? = null,
                  val parameters: Array<String>? = null)

open class ResourceSupport(
        @SerializedName("_links") private val links : Map<String, LinkEntry> = HashMap()){

    fun getLink(key: String): LinkEntry? {
        return links[key]
    }

}

class LinkEntry(val href: String) {

    val actionId: Long?
        get() {
            var url: URL?
            try {
                url = URL(href)
            } catch (e: MalformedURLException) {
                e.printStackTrace()
                return null
            }

            val path = url.path.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            var actionId: Long? = null
            if (path.size > 6) {
                actionId = java.lang.Long.valueOf(path[6])
            }

            return actionId
        }
}