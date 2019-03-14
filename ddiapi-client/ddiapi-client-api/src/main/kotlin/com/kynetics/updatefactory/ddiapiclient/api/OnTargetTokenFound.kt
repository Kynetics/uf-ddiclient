package com.kynetics.updatefactory.ddiapiclient.api

//TODO rename interface and method on ddi client builder
interface OnTargetTokenFound {
    fun onFound(targetToken: String)
}