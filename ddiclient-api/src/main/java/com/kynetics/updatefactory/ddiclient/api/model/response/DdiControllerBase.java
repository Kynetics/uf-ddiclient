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

/**
 * {@link DdiControllerBase} resource content.
 *
 * @author Daniele Sergio
 */
public class DdiControllerBase extends ResourceSupport {

    private DdiConfig config;

    public DdiConfig getConfig() {
        return config;
    }

}
