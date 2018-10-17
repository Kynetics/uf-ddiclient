/*
 * Copyright © 2017-2018 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.updatefactory.ddiclient.example.callback;

import com.kynetics.updatefactory.ddiclient.core.UFService;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniele Sergio
 */
public class TargetDataMock implements UFService.TargetData {
    @Override
    public Map<String, String> get() {
        final Map<String, String> map = new HashMap<>();
        map.put("test", "tes");
        return map;
    }
}
