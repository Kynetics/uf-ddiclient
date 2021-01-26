/*
 * Copyright © 2017-2021  Kynetics  LLC
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package com.kynetics.updatefactory.ddiclient.core

import java.io.File
import java.lang.StringBuilder
import java.security.DigestInputStream
import java.security.MessageDigest

fun File.md5(): String {
    val md = MessageDigest.getInstance("MD5")
    val sb = StringBuilder()

    inputStream().use { fis ->
        val digestInputStream = DigestInputStream(fis, md)
        val buffer = ByteArray(4096)
        while (digestInputStream.read(buffer) > -1) {}
        digestInputStream.close()
        digestInputStream.messageDigest
            .digest()
            .forEach {
                sb.append(String.format("%02X", it))
            }
    }
    return sb.toString().toLowerCase()
}