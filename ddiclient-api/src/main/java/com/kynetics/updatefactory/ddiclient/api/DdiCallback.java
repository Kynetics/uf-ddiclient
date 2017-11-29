/*
 * Copyright © 2017 Kynetics LLC
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
import com.kynetics.updatefactory.ddiclient.api.model.response.Error;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;

import static com.kynetics.updatefactory.ddiclient.api.api.DdiRestConstants.TARGET_TOKEN_HEADER_NAME;

public abstract class DdiCallback<T> implements Callback<T> {

    @Override
    public void onResponse(Call<T> call, Response<T> response) {
        if(response.isSuccessful()){
            onSuccess(response.body());
            _onTargetTokenReceived(response.headers().get(TARGET_TOKEN_HEADER_NAME));
        } else {
            final Gson gson = new Gson();
            try {
                final String errorString = response.errorBody().string();
                System.out.println(errorString);
                Error error = gson.fromJson(errorString, Error.class);
                error = error == null ? new Error() : error;
                error.setCode(response.code());
                onError(error);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void _onTargetTokenReceived(String value){
        if(value == null || value.isEmpty()){
            return;
        }
        onTargetTokenReceived(value);
    }

    protected void onTargetTokenReceived(String value){
        System.out.println(String.format("Target token: %s", value));
    }

    protected abstract void onError(Error error);
    protected abstract void onSuccess(T response);

}
