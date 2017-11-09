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

import com.kynetics.updatefactory.ddiclient.api.model.response.ResourceSupport;

import java.io.Serializable;

/**
 * @author Daniele Sergio
 */
public class FileInfo implements Serializable {

    private static final long serialVersionUID = 1191642138337839065L;
    private final ResourceSupport.LinkEntry.LinkInfo linkInfo;
    private final String shae1;
    private final String md5;

    public FileInfo(ResourceSupport.LinkEntry.LinkInfo linkInfo, String shae1, String md5) {
        this.linkInfo = linkInfo;
        this.shae1 = shae1;
        this.md5 = md5;
    }

    public ResourceSupport.LinkEntry.LinkInfo getLinkInfo() {
        return linkInfo;
    }

    public String getShae1() {
        return shae1;
    }

    public String getMd5() {
        return md5;
    }
}
