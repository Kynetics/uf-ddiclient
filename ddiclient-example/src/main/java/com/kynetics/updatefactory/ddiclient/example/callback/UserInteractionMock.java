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

import com.kynetics.updatefactory.ddiclient.core.servicecallback.UserInteraction;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Future;

/**
 * @author Daniele Sergio
 */
public class UserInteractionMock implements UserInteraction {

    @Override
    public Future<Boolean> grantAuthorization(Authorization auth) {
        final AuthorizationResponse response = new AuthorizationResponse();
        new Timer().schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        response.put(Boolean.TRUE);
                    }
                },
                20000
        );
        return response;
    }
}
