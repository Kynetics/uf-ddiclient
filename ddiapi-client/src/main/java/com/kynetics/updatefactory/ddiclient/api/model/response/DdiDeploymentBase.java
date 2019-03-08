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

/**
 * Update action resource.
 *
 * @author Daniele Sergio
 */
public class DdiDeploymentBase extends ResourceSupport {

    @SerializedName("id")
    private String deplyomentId;

    @SerializedName("deployment")
    private DdiDeployment deployment;

    /**
     * Action history containing current action status and a list of feedback
     * messages received earlier from the controller.
     */
    @SerializedName("actionHistory")
    private DdiActionHistory actionHistory;

    public DdiDeployment getDeployment() {
        return deployment;
    }

    /**
     * Returns the action history containing current action status and a list of
     * feedback messages received earlier from the controller.
     *
     * @return {@link DdiActionHistory}
     */
    public DdiActionHistory getActionHistory() {
        return actionHistory;
    }

    @Override
    public String toString() {
        return "DeploymentBase [id=" + deplyomentId + ", deployment=" + deployment + " actionHistory="
                + actionHistory.toString() + "]";
    }

}
