/*
 * Copyright © 2017 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.updatefactory.ddiclient.api.validation;

/**
 * @author Daniele Sergio
 */
public class Assert {
    private Assert() {
        throw new AssertionError();
    }

    public static void NotEmpty(String item, String itemName) {
        if (item == null || item.isEmpty()) {
            throw new IllegalStateException(String.format("%s could not be null or empty", itemName));
        }
    }

    public static void NotNull(Object item, String itemName) {
        if (item == null) {
            throw new IllegalStateException(String.format("%s could not be null", itemName));
        }
    }
}
