/*
 * Copyright © 2017 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.updatefactory.ddiclient.api.model.response;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
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

        public LinkInfo parseLink(){
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

            return  new LinkInfo(path[1],path[4],actionId,null,null);
        }

        public LinkInfo parseLink2(){
            URL url = null;
            try {
                url = new URL(href);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            final String[] path = url.getPath().split("/");
            return  new LinkInfo(path[1],path[4],null,Long.valueOf(path[6]),path[8]);
        }

        public static class LinkInfo implements Serializable{

            private static final long serialVersionUID = -7999229335657306366L;
            private final String tenant;
            private final String controllerId;
            private final Long actionId;
            private final Long softwareModules;
            private final String fileName;

            public LinkInfo(String tenant, String controllerId, Long actionId, Long softwareModules, String fileName) {
                this.tenant = tenant;
                this.controllerId = controllerId;
                this.actionId = actionId;
                this.softwareModules = softwareModules;
                this.fileName = fileName;
            }

            public String getTenant() {
                return tenant;
            }

            public String getControllerId() {
                return controllerId;
            }

            public Long getActionId() {
                return actionId;
            }

            public Long getSoftwareModules() {
                return softwareModules;
            }

            public String getFileName() {
                return fileName;
            }
        }
    }
}
