/*
 * Copyright © 2017-2018 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.updatefactory.ddiclient.core.servicecallback;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Daniele Sergio
 */
public interface UserInteraction {
    Future<Boolean> grantAuthorization(Authorization auth);

    enum Authorization {
        DOWNLOAD, UPDATE
    }

    class AuthorizationResponse implements Future<Boolean> {
        private final CountDownLatch latch = new CountDownLatch(1);
        private Boolean value;

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return latch.getCount() == 0;
        }

        @Override
        public Boolean get() throws InterruptedException {
            latch.await();
            return value;
        }

        @Override
        public Boolean get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
            if (latch.await(timeout, unit)) {
                return value;
            } else {
                throw new TimeoutException();
            }
        }

        public void put(Boolean result) {
            _put(result);
        }

        private void _put(Boolean result) {
            value = result;
            latch.countDown();
        }
    }
}
