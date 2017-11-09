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

import com.kynetics.updatefactory.ddiclient.api.model.response.ResourceSupport;
import com.kynetics.updatefactory.ddiclient.api.model.response.ResourceSupport.LinkEntry.LinkInfo;

import java.io.Serializable;

/**
 * @author Daniele Sergio
 */
public class FileInfo implements Serializable {

    private static final long serialVersionUID = 1191642138337839065L;
    private final ResourceSupport.LinkEntry.LinkInfo linkInfo;
    private Hash hash;

    public FileInfo(LinkInfo linkInfo, Hash hash) {
        this.linkInfo = linkInfo;
        this.hash = hash;
    }

    public ResourceSupport.LinkEntry.LinkInfo getLinkInfo() {
        return linkInfo;
    }

    public Hash getHash() {
        return hash;
    }
}
