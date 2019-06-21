package com.kynetics.updatefactory.ddiclient.core.api

interface EventListener {

    fun onEvent(event:Event)

    sealed class Event {
        override fun toString(): String {
            return this.javaClass.simpleName
        }

        data class Downloading(val percentage:Double = 0.0): Event()
        data class StartDownloadFile(val fileName: String):Event()
        data class FileDownloaded(val fileDownloaded:String): Event()
        object AllFilesDownloaded: Event()
        object Updating: Event()
        object UpdateCancelled: Event()
        data class UpdateFinished(val successApply: Boolean, val details:List<String>): Event()
        data class Error(val message:List<String>) : Event()
        object Polling:Event()
        object Idle:Event()
        object WaitingDownloadAuthorization:Event()
        object WaitingUpdateAuthorization:Event()
    }
}