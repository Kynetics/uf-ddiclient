package com.kynetics.updatefactory.ddiclient.core.api


interface TargetTokenFoundListener {

    fun onFound(targetToken: String) = {}
}