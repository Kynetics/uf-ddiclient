/*
 * Copyright © 2017-2018 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.updatefactory.ddiclient.core.model;

import com.kynetics.updatefactory.ddiclient.api.model.response.ResourceSupport;
import com.kynetics.updatefactory.ddiclient.api.model.response.ResourceSupport.LinkEntry.LinkInfo;

import java.io.Serializable;

/**
 * @author Daniele Sergio
 */
public class FileInfo implements Serializable {

    private static final long serialVersionUID = -4430835128376273893L;
    private final ResourceSupport.LinkEntry.LinkInfo linkInfo;
    private final Hash hash;
    private final long size;

    public FileInfo(LinkInfo linkInfo, Hash hash, long size) {
        this.linkInfo = linkInfo;
        this.hash = hash;
        this.size = size;
    }

    public ResourceSupport.LinkEntry.LinkInfo getLinkInfo() {
        return linkInfo;
    }

    public Hash getHash() {
        return hash;
    }

    public long getSize() {
        return size;
    }
}
