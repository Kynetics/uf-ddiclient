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

import com.kynetics.updatefactory.ddiclient.api.security.UpdateFactoryAuthenticationRequestInterceptor;

/**
 * @author Daniele Sergio
 */
public class OnTargetTokenFoundMock implements UpdateFactoryAuthenticationRequestInterceptor.OnTargetTokenFound {
    @Override
    public void onFound(String targetToken) {

    }
}