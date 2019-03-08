/*
 * Copyright © 2017-2019 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.redux

import com.kynetics.redux.api.*
import org.slf4j.LoggerFactory



/**
 * @author Daniele Sergio
 */

object Utils {

    private val LOGGER = LoggerFactory.getLogger(Utils::class.java)

    fun <S: State<*>, A1: Action<*>, A2: Action<*>, R> createStore(reducer: ReducerType<S, A2>, initialState: S, enhancer: EnhancerType<S, A1, A1, A2, R>): Store<S, A2, R> {
        val sc : StoreCreatorType<S, A1, A1> = { r, i ->
            Store.create(r, i)
        }
        val store = enhancer.invoke(sc).invoke(reducer, initialState)
        LOGGER.info("Store created")
        return store
    }

    fun<S:State<*>, A:Action<*>> applyMiddleware(vararg middlewares: MiddlewareType<S, A, A, A, A>): EnhancerType<S, A, A, A, A> {
        LOGGER.info("Applying Middlewares")
        return { storeCreator ->
            { reducer: ReducerType<S, A>, state: S ->
                val store: Store<S, A, A> = storeCreator.invoke(reducer, state)

                val dispatcher: Dispatcher<A, A> = middlewares.reversed().fold(store as MiddlewareApi<S, A, A>) { acc, middleware ->
                    object : MiddlewareApi<S, A, A> by store {
                        override fun dispatch(action: A): A {
                            return middleware.invoke(store).invoke(acc::dispatch).invoke(action)
                        }
                    }
                }
                object : Store<S, A, A> by store{
                    override fun dispatch(action: A): A {
                        return dispatcher.dispatch(action)
                    }
                }

            }

        }
    }

    fun <S: State<*>, A: Action<*>> combineReducers(vararg reducers: ReducerType<S, A>): ReducerType<S, A> {
        LOGGER.info("Combining Reducers")
        return reducers.reduceRight{ r, ele ->
            { s, a ->
                ele.invoke(r.invoke(s,a),a)
            }
        }
    }

}

