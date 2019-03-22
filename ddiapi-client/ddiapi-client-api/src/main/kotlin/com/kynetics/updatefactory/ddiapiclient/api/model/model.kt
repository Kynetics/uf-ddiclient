package com.kynetics.updatefactory.ddiapiclient.api.model

import com.google.gson.annotations.SerializedName
import java.time.Instant

/*======================================================================================================================
 *==== REQUESTS ========================================================================================================
 *====================================================================================================================*/

data class CnclFdbkReq(
        val id: String,
        val time: String,
        val status: Sts){
    data class Sts(
            val execution: Exc,
            val result: Rslt,
            val details: List<String>) {
        enum class Exc{
            closed,
            proceeding,
            canceled,
            scheduled,
            rejected,
            resumed}
        data class Rslt(
                val finished: Fnsh){
            enum class Fnsh{
                success,
                failure,
                none}
        }
    }
    companion object {
        fun newInstance(id: String,
                        execution: CnclFdbkReq.Sts.Exc,
                        finished: CnclFdbkReq.Sts.Rslt.Fnsh,
                        vararg messages: String):CnclFdbkReq {
            return CnclFdbkReq(id, Instant.now().toString(), CnclFdbkReq.Sts(
                    execution,
                    CnclFdbkReq.Sts.Rslt(
                            finished),
                    messages.toList()
            ))
        }
    }
}

data class DeplFdbkReq(
        val id: String,
        val time: String,
        val status: Sts) {
    data class Sts(
            val execution: Exc,
            val result: Rslt,
            val details: List<String>) {
        enum class Exc {
            closed,
            proceeding,
            canceled,
            scheduled,
            rejected,
            resumed}
        data class Rslt(
                val finished: Fnsh,
                val progress: Prgrs?) {
            enum class Fnsh {
                success,
                failure,
                none}
            data class Prgrs(
                    val of: Int,
                    val cnt: Int)
        }
    }

    companion object {
        fun newInstance(id: String,
                        execution: Sts.Exc,
                        progress: Sts.Rslt.Prgrs,
                        finished: Sts.Rslt.Fnsh,
                        vararg messages: String):DeplFdbkReq {
            return DeplFdbkReq(id, Instant.now().toString(),Sts(
                    execution,
                    Sts.Rslt(
                            finished,
                            progress),
                    messages.toList()
            ))
        }
    }
}

data class CfgDataReq(
        val id: String,
        val time: String,
        val status: Sts,
        val data: Map<String,String>?,
        val mode: Mod){
    data class Sts(
            val execution: Exc,
            val result: Rslt,
            val details: List<String>) {
        enum class Exc{
            closed,
            proceeding,
            canceled,
            scheduled,
            rejected,
            resumed}
        data class Rslt(
                val finished: Fnsh){
            enum class Fnsh{
                success,
                failure,
                none}
        }
    }
    enum class Mod{
        merge,
        replace,
        remove }

    companion object {
        fun of(map: Map<String, String>?=null, mod: Mod) = CfgDataReq(
                "",
                "20140511T121314",//Instant.now().toString(),
                Sts(
                        Sts.Exc.closed,
                        Sts.Rslt(
                                Sts.Rslt.Fnsh.success),
                                emptyList()
                        ),
                    map,
                    mod
                )
    }

}



/*======================================================================================================================
 *==== RESPONSES =======================================================================================================
 *====================================================================================================================*/



data class CnclActResp(
        val id: String,
        val cancelAction: Act){
    data class Act(
            val stopId: String
    )
}

data class CtrlBaseResp(
        val config: Cfg,
        val _links: Lnks?) {
    data class Cfg(
            val polling: Polling){
        data class Polling(
                val sleep: String)
    }
    data class Lnks(
            val deploymentBase: Lnk?,
            val cancelAction: Lnk?,
            val configData: Lnk?){
        data class Lnk(
                val href: String)
    }
    fun requireConfigData() = _links?.configData != null
    fun requireDeployment() = _links?.deploymentBase != null
    fun requireCancel() = _links?.cancelAction != null
    fun deploymentActionId() = xxxAid("deploymentBase")
    fun cancelActionId() = xxxAid("cancelAction")
    private fun xxxAid(pfx:String) =
            ".*$pfx/([a-zA-Z0-9_]+)"
            .toRegex()
            .find(_links?.deploymentBase?.href ?: _links?.cancelAction?.href ?: "")
            ?.destructured
            ?.component1()!!
}

data class DeplBaseResp(
        val id: String,
        val deployment: Depl,
        val actionHistory: Hist?){
    data class Depl(
            val download:Appl,
            val update: Appl,
            val maintenanceWindow: MaintWind,
            val chunks: Set<Cnk>){
        enum class Appl{
            skip,
            attempt,
            forced}
        enum class MaintWind{
            available,
            unavailable}
        data class Cnk(
                val metadata:Set<Mtdt>?,
                val part: String,
                val name: String,
                val version: String,
                val artifacts: Set<Artfct>){
            data class Mtdt(
                    val key: String,
                    val value: String)
            data class Artfct(
                    val filename: String,
                    val hashes: Hshs,
                    val size: Long,
                    val _links: Lnks){
                data class Hshs(
                        val sha1: String,
                        val md5: String)
                data class Lnks(
                        val download: Lnk,
                        val md5sum: Lnk,
                        @SerializedName("download-http")
                        val download_http: Lnk,
                        @SerializedName("md5sum-http")
                        val md5sum_http: Lnk){
                    data class Lnk(val href: String)
                }
            }
        }
    }
    data class Hist(
            val status: Sts?,
            val messages: List<String>){
        enum class Sts{
            CANCELED,
            WARNING,
            ERROR,
            FINISHED,
            RUNNING}
    }
}

data class ArtfctResp(
        val filename: String,
        val hashes: Hshs,
        val size: Long,
        val _links: Lnks){
    data class Hshs(
            val sha1: String,
            val md5: String)
    data class Lnks(
            val download: Lnk,
            val md5sum: Lnk,
            @SerializedName("download-http")
            val download_http: Lnk,
            @SerializedName("md5sum-http")
            val md5sum_http: Lnk){
        data class Lnk(val href: String)
    }
}
