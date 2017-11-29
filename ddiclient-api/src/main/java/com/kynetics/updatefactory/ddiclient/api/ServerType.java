/*
 * Copyright © 2017 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.updatefactory.ddiclient.api;

import com.kynetics.updatefactory.ddiclient.api.security.Authentication;
import com.kynetics.updatefactory.ddiclient.api.security.HawkbitAuthenticationRequestInterceptor;
import com.kynetics.updatefactory.ddiclient.api.security.UpdateFactoryAuthenticationRequestInterceptor;
import okhttp3.Interceptor;

import java.util.List;

/**
 * @author Daniele Sergio
 */
public enum ServerType {
    UPDATE_FACTORY {
        @Override
        Interceptor getAuthenticationRequestInterceptor(List<Authentication> authentications) {
            return new UpdateFactoryAuthenticationRequestInterceptor(authentications);
        }
    }, HAWKBIT {
        @Override
        Interceptor getAuthenticationRequestInterceptor(List<Authentication> authentications) {
            return new HawkbitAuthenticationRequestInterceptor(authentications);
        }
    };

    abstract Interceptor getAuthenticationRequestInterceptor(List<Authentication> authentications);
}
