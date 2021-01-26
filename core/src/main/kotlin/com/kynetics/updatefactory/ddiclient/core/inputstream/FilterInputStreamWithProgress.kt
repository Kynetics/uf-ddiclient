/*
 * Copyright © 2017-2021  Kynetics  LLC
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package com.kynetics.updatefactory.ddiclient.core.inputstream

import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.atomic.AtomicInteger

class FilterInputStreamWithProgress(
    inputStream: InputStream,
    private val totalSize: Long
) : FilterInputStream(inputStream) {

    private var alreadyRead: AtomicInteger = AtomicInteger(0)

    @Throws(IOException::class)
    override fun read(): Int {
        try {
            val count = this.`in`.read()
            alreadyRead.addAndGet(count)
            return count
        } catch (e: IOException) {
            throw e
        }
    }

    @Throws(IOException::class)
    override fun read(var1: ByteArray, var2: Int, var3: Int): Int {
        try {
            val count = this.`in`.read(var1, var2, var3)
            alreadyRead.addAndGet(count)
            return count
        } catch (e: IOException) {
            throw e
        }
    }

    fun getProgress(): Double {
        return alreadyRead.get().toDouble() / totalSize
    }
}
