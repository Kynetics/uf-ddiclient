/*
 * Copyright © 2017 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.kynetics.updatefactory.ddiclient.api.api;

/**
 * Constants for the direct device integration rest resources.
 *
 * @author Daniele Sergio
 */
public final class DdiRestConstants {

    /**
     * The base URL mapping of the direct device integration rest resources.
     */
    public static final String BASE_V1_REQUEST_MAPPING = "/{tenant}/controller/v1";

    /**
     * Deployment action resources.
     */
    public static final String DEPLOYMENT_BASE_ACTION = "deploymentBase";

    /**
     * Cancel action resources.
     */
    public static final String CANCEL_ACTION = "cancelAction";

    /**
     * Feedback channel.
     */
    public static final String FEEDBACK = "feedback";

    /**
     * File suffix for MDH hash download (see Linux md5sum).
     */
    public static final String ARTIFACT_MD5_DWNL_SUFFIX = ".MD5SUM";

    /**
     * Config data action resources.
     */
    public static final String CONFIG_DATA_ACTION = "configData";

    /**
     * Default value specifying that no action history to be sent as part of
     * response to deploymentBase
     * {@link DdiRestApi#getControllerBasedeploymentAction}.
     */
    public static final int NO_ACTION_HISTORY = 0;

    public static final int DEFAULT_RESOURCE = -1;

    private DdiRestConstants() {
        // constant class, private constructor.
    }
}
