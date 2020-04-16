package com.kynetics.updatefactory.ddiclient.virtualdevice

import org.joda.time.Duration
import java.util.UUID

object Configuration {

    val logelLevel = env("UF_LOG_LEVEL", "TRACE")
    val poolSize = env("UF_CLIENT_POOL_SIZE", "1").toInt()
    val tenant = env("UF_TENANT", "TEST")
    val controllerId = env("UF_CONTROLLER_ID", UUID.randomUUID().toString())
    val url = env("UF_URL", "https://stage.updatefactory.io")
    val gatewayToken = env("UF_GATEWAY_TOKEN", "")

    val virtualDeviceStartingDelay = Duration.standardSeconds(
        env("UF_VIRTUAL_DEVICE_STARTING_DELAY", "1").toLong()
    ).millis

    val storagePath = env("UF_STORAGE_PATH", "/client")
    val targetAttributes = env("UF_TARGET_ATTRIBUTES","client,kotlin virtual device")
    val logMessage = env("UF_LOG_MESSAGE", "{4}")

    val srvMsgTemplateBeforeUpdate = env("UF_SRV_MSF_BEFORE_UPDATE", "Applying the sw {0} for target {1}")
    val srvMsgTemplateAfterUpdate = env("UF_SRV_MSF_AFTER_UPDATE","Applied the sw {0} for target {1}")

    val grantDownload = env("UF_GRANT_DOWNLOAD", "true").toBoolean()
    val grantUpdate = env("UF_GRANT_UPDATE", "true").toBoolean()

    private fun env(envVariable:String, defaultValue:String):String{
        return System.getenv(envVariable) ?: defaultValue
    }

}