/*
 * Copyright © 2017-2019 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.kynetics.updatefactory.ddiclient.api.model.response;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * @author Daniele Sergio
 */
public class DdiActionHistory {

    @SerializedName("status")
    private String actionStatus;

    @SerializedName("messages")
    private List<String> messages;


    public String getActionStatus() {
        return actionStatus;
    }

    public List<String> getMessages() {
        return messages;
    }

    @Override
    public String toString() {
        return "Action history [" + "status=" + actionStatus + ", messages={" + messages.toString() + "}]";
    }

}
