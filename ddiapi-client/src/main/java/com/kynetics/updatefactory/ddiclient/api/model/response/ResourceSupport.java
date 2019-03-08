/*
 * Copyright © 2017-2019 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.updatefactory.ddiclient.api.model.response;

import com.google.gson.annotations.SerializedName;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniele Sergio
 */
public class ResourceSupport {
    @SerializedName("_links")
    private  Map<String, LinkEntry> links = new HashMap<>();

    public LinkEntry getLink(String key){
        return links.get(key);
    }

    public static class LinkEntry {

        private String href;

        public String getHref() {
            return href;
        }

        public Long getActionId(){
            URL url = null;
            try {
                url = new URL(href);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            final String[] path = url.getPath().split("/");
            Long actionId = null;
            if(path.length >6){
                actionId = Long.valueOf(path[6]);
            }

            return  actionId;
        }
    }
}
