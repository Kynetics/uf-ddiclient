/*
 * Copyright © 2017 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.updatefactory.ddiclient.core.model;

import java.io.Serializable;

/**
 * @author Daniele Sergio
 */
public class Hash implements Serializable{

    private static final long serialVersionUID = -5692148452359794783L;
    private final String md5;
    private final String sha1;

    public Hash(String md5, String sha1) {
        this.md5 = md5;
        this.sha1 = sha1;
    }

    public String getMd5() {
        return md5;
    }

    public String getSha1() {
        return sha1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Hash hash = (Hash) o;

        if (md5 != null ? !md5.equals(hash.md5) : hash.md5 != null) return false;
        return sha1 != null ? sha1.equals(hash.sha1) : hash.sha1 == null;
    }

    @Override
    public int hashCode() {
        int result = md5 != null ? md5.hashCode() : 0;
        result = 31 * result + (sha1 != null ? sha1.hashCode() : 0);
        return result;
    }
}
