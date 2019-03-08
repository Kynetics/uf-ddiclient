/*
 * Copyright © 2017-2019 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.redux.api

/**
 * @author Daniele Sergio
 */
interface Middleware<S: State<*>, A1: Action<*>, R1, A2: Action<*>, R2>{
    fun apply(middlewareApi: MiddlewareApi<S, A1, R1>):  (DispatcherType<A1, R1>) -> DispatcherType<A2, R2>
}

