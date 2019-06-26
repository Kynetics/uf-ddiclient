package com.kynetics.updatefactory.ddiclient.core.api

interface MessageListener {

    fun onMessage(message:Message)

    sealed class Message(val description: String) {
        override fun toString(): String {
            return this.javaClass.simpleName
        }

        sealed class State(description: String):Message(description){
            object Downloading: State("Client is downloading artifacts from server")
            object Updating: State("The update process is started. Any request to cancel an update will be rejected")
            object CancellingUpdate: State("Last update request is being cancelled")
            object WaitingDownloadAuthorization: State("Waiting authorization to start download")
            object WaitingUpdateAuthorization: State("Waiting authorization to start update")
            data class Error(val details:List<String>) : State("An error is occurred")
            object Waiting: State("There isn't any request from server")
        }

        sealed class Event(description: String):Message(description){
            object Polling: Event("Client is contacting server to retrieve new action to execute")
            data class StartDownloadFile(val fileName: String): Event("A file downloading is started")
            data class FileDownloaded(val fileDownloaded:String): Event("A file is downloaded")
            data class DownloadProgress(val fileName: String, val percentage:Double = 0.0): Event("Percent of file downloaded")
            object AllFilesDownloaded: Event("All file needed are downloaded")
            data class UpdateFinished(val successApply: Boolean, val details:List<String>): Event("The update is finished")
        }

    }
}