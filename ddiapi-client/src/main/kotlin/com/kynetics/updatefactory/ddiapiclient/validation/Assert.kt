/*
 * Copyright © 2017-2019 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.updatefactory.ddiapiclient.validation

import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL

/**
 * @author Daniele Sergio
 */
class Assert private constructor() {
    init {
        throw AssertionError()
    }

    companion object {

        fun NotEmpty(item: String?, itemName: String) {
            if (item == null || item.isEmpty()) {
                throw IllegalStateException(String.format("%s could not be null or empty", itemName))
            }
        }

        fun NotNull(item: Any?, itemName: String) {
            if (item == null) {
                throw IllegalStateException(String.format("%s could not be null", itemName))
            }
        }

        fun ValidateUrl(url: String?, itemName: String) {
            try {
                val test = URL(url)
                test.toURI()
            } catch (e: MalformedURLException) {
                throw IllegalStateException(String.format("%s is a malformed url", itemName))
            } catch (e: URISyntaxException) {
                throw IllegalStateException(String.format("%s is a malformed url", itemName))
            }

        }
    }
}
