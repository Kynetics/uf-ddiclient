/*
 * Copyright © 2017-2018 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.updatefactory.ddiclient.example.observer;

import com.kynetics.updatefactory.ddiclient.core.UFService;

import java.util.Observable;
import java.util.Observer;

/**
 * @author Daniele Sergio
 */
public class ObserverState implements Observer {

    @Override
    public void update(Observable observable, Object o) {
        if (o instanceof UFService.SharedEvent) {
            final UFService.SharedEvent eventNotify = (UFService.SharedEvent) o;
            System.out.println(String.format("(%s,%s) -> %s",
                    eventNotify.getOldState().getStateName(),
                    eventNotify.getEvent().getEventName(),
                    eventNotify.getNewState().getStateName()));


        }
    }
}
