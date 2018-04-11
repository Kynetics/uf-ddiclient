/*
 * Copyright © 2017-2018 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.updatefactory.ddiclient.core.filterinputstream;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Daniele Sergio
 */
public class NotifyStatusFilterInputStream extends FilterInputStream {

    public interface Notifier{
        void notify(double percent);
    }

    public NotifyStatusFilterInputStream(InputStream inputStream, long totalSize, Notifier notifier) {
        super(inputStream);
        this.inputStream = inputStream;
        this.totalSize = totalSize;
        this.notifier = notifier;
    }

    @Override
    public int read() throws IOException {
        try {
            final int count = this.in.read();
            notify(count);
            return count;
        }catch (IOException e){
            throw e;
        }
    }

    @Override
    public int read(byte[] var1, int var2, int var3) throws IOException {
        try {
            final int count = this.in.read(var1, var2, var3);
            notify(count);
            return count;
        }catch (IOException e){
            throw e;
        }
    }

    private void notify(int count) {
        notifier.notify(count == -1 ? 1.0 :  (olreadyRead *1.0 / totalSize));
        olreadyRead+=count;
    }

    private final InputStream inputStream;
    private final long totalSize;
    private long olreadyRead;
    private final Notifier notifier;

}
