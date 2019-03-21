package com.kynetics.updatefactory.ddiclient.core.test

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import kotlinx.coroutines.Deferred
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.Executors

/**
 * @author Daniele Sergio
 */

interface IdentifiableObject{
    val id:Long?
}
typealias ObjectDeletion = (String, Long) -> Call<Void>

data class Error(val exceptionClass:String = "", val errorCode:String = "", val message:String = "Error")
data class SoftwareModule(val vendor:String, val name:String, val description:String, val type: String, val version:String, override val id:Long? = null) : IdentifiableObject
data class Distribution(val name:String, val description:String, val type: String, val version:String, val modules:Set<Id>, override val id:Long? = null, val requiredMigrationStep:Boolean = false) : IdentifiableObject
data class Id(val id: Long)
data class ObjectType(val name:String, val description:String, val key:String)
data class WrapperList(val content:Array<ObjectType>)


data class ActionStatus(val content:Set<ContentEntry>, val total: Int = content.size, val size: Int = content.size){
    data class ContentEntry(val type: Type, val messages:List<String?>){
        enum class Type{
            finished, error, warning, pending, running, canceled, retrieved, canceling, download
        }
    }
}

interface ManagementApi {
    companion object {
        const val BASE_V1_REQUEST_MAPPING = "/rest/v1"


    }


    @GET("$BASE_V1_REQUEST_MAPPING/targets/{targetId}/actions/{actionId}/status")
    fun getTargetActionStatusAsync(@Header("Authorization") auth: String,
                                   @Path("targetId") targetId:String,
                                   @Path("actionId") actionId:Int): Deferred<ActionStatus>



    @DELETE("$BASE_V1_REQUEST_MAPPING/targets/{targetId}/actions/{actionId}")
    fun deleteTargetActionAsync(@Header("Authorization") auth: String,
                                @Path("targetId") targetId:String,
                                @Path("actionId") actionId:Int) : Deferred<Unit>

}


object ManagementClient {

    fun newInstance(url:String): ManagementApi {
            return object : ManagementApi {
                private val delegate: ManagementApi = Retrofit.Builder().baseUrl(url)
                        .client( OkHttpClient.Builder().build())
                        .addConverterFactory(GsonConverterFactory.create())
                        .addCallAdapterFactory(CoroutineCallAdapterFactory())
                        .callbackExecutor(Executors.newSingleThreadExecutor())
                        .build()
                        .create(ManagementApi::class.java)

                override fun getTargetActionStatusAsync(auth: String, targetId: String, actionId: Int): Deferred<ActionStatus> {
                    return delegate.getTargetActionStatusAsync(auth, targetId, actionId)
                }

                override fun deleteTargetActionAsync(auth: String, targetId: String, actionId: Int): Deferred<Unit> {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }
            }
    }

}