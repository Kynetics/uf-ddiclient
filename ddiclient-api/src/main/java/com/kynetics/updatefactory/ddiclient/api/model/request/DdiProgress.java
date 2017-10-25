/*
 * Copyright © 2017 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */
package com.kynetics.updatefactory.ddiclient.api.model.request;

/**
 * Action fulfillment progress by means of gives the achieved amount of maximal
 * of possible levels.
 *
 * @author Daniele Sergio
 */
public class DdiProgress {

    private final int cnt;

    private final int of;

    public DdiProgress(int cnt, int of) {
        this.cnt = cnt;
        this.of = of;
    }

    public int getCnt() {
        return cnt;
    }

    public int getOf() {
        return of;
    }

    @Override
    public String toString() {
        return "Progress [cnt=" + cnt + ", of=" + of + "]";
    }

}
