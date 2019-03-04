/*
 * Copyright © 2017-2019 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.updatefactory.ddiapiclient.model.request

import com.kynetics.updatefactory.ddiapiclient.model.DdiStatus

data class DdiActionFeedback(val id: Long, val time: String, val status: DdiStatus)

data class DdiConfigData(val id: Long, val time: String, val status: DdiStatus, val data: Map<String, String>)