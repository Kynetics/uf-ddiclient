/*
 * Copyright © 2017 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.updatefactory.core.model;

/**
 * @author Daniele Sergio
 */
public class Hash {

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
}
