/*
 * Copyright © 2017 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.updatefactory.ddiclient.api.model.request;

/**
 * @author Daniele Sergio
 */
public class DdiActionFeedback {
    private final Long id;

    private final String time;

    private final DdiStatus status;

    public DdiActionFeedback(Long id, String time, DdiStatus status) {
        this.id = id;
        this.time = time;
        this.status = status;
    }

    public long getId() {
        return id;
    }

    public String getTime() {
        return time;
    }

    public DdiStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "ActionFeedback [id=" + id + ", time=" + time + ", status=" + status + "]";
    }

}
