/*
 * Copyright © 2017-2018 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.updatefactory.ddiclient.api.model.response;

import com.google.gson.annotations.SerializedName;

/**
 * @author Daniele Sergio
 */
public class Error {

    @SerializedName(value="errorCode",alternate = "status")
    private String errorCode;

    @SerializedName(value="exceptionClass",alternate = "exception")
    private String exceptionClass;

    private int code;

    private String message;

    private String[] parameters;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getExceptionClass() {
        return exceptionClass;
    }

    public String getMessage() {
        return message;
    }

    public String[] getParameters() {
        return parameters;
    }

}