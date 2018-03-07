/*
 * Copyright © 2017-2018 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.kynetics.updatefactory.ddiclient.api.model.request;

import java.util.Map;

/**
 * Feedback channel for ConfigData action.
 *
 * @author Daniele Sergio
 */
public class DdiConfigData extends DdiActionFeedback {

    private final Map<String, String> data;

    public DdiConfigData(Long id, String time, DdiStatus status, Map<String, String> data) {
        super(id, time, status);
        this.data = data;
    }

    public Map<String, String> getData() {
        return data;
    }

    @Override
    public String toString() {
        return "ConfigData [data=" + data + ", toString()=" + super.toString() + "]";
    }

}
