/*
 * Copyright © 2017-2019 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.updatefactory.ddiapiclient

/**
 * @author Daniele Sergio
 */
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.kynetics.updatefactory.ddiapiclient.model.response.Error
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import java.io.IOException

abstract class DdiCallback<T> : Callback<T> {

    override fun onResponse(call: Call<T>, response: Response<T>) {
        if (response.isSuccessful) {
            onSuccess(response.body())
        } else {
            val gson = Gson()
            try {
                val errorString = response.errorBody()!!.string()
                println(errorString)
                var error: Error? = if (isJsonValid(gson, errorString)) gson.fromJson<Error>(errorString, Error::class.java) else Error()
                error = error ?: Error()
                error.code = response.code()
                onError(error)
            } catch (e: IOException) {
                e.printStackTrace()
                val error = Error()
                error.code = response.code()
                onError(error)
            }

        }
    }

    private fun isJsonValid(gson: Gson, jsonInString: String): Boolean {
        return try {
            gson.fromJson<Any>(jsonInString, Any::class.java)
            true
        } catch (ex: JsonSyntaxException) {
            false
        } catch (ex: IllegalStateException) {
            false
        }

    }

    abstract fun onError(error: Error)
    abstract fun onSuccess(response: T?)

}
