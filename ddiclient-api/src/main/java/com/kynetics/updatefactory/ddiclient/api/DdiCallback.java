/*
 * Copyright © 2017-2018 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.updatefactory.ddiclient.api;
/**
 * @author Daniele Sergio
 */
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.kynetics.updatefactory.ddiclient.api.model.response.Error;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;

public abstract class DdiCallback<T> implements Callback<T> {

    @Override
    public void onResponse(Call<T> call, Response<T> response) {
        if(response.isSuccessful()){
            onSuccess(response.body());
        } else {
            final Gson gson = new Gson();
            try {
                final String errorString = response.errorBody().string();
                System.out.println(errorString);
                Error error = isJsonValid(gson, errorString) ? gson.fromJson(errorString, Error.class) : new Error() ;
                error = error == null ? new Error() : error;
                error.setCode(response.code());
                onError(error);
            } catch (IOException e) {
                e.printStackTrace();
                final Error error = new Error();
                error.setCode(response.code());
                onError(error);
            }
        }
    }

    private static boolean isJsonValid(Gson gson, String jsonInString) {
        try {
            gson.fromJson(jsonInString, Object.class);
            return true;
        } catch(JsonSyntaxException | IllegalStateException ex) {
            return false;
        }
    }

    public abstract void onError(Error error);
    public abstract void onSuccess(T response);

}
