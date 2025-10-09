package com.joinforage.forage.android.core.services.telemetry

import android.content.Context
import com.joinforage.datadog.android.Datadog
import com.joinforage.datadog.android.core.configuration.Configuration
import com.joinforage.datadog.android.log.Logger
import com.joinforage.datadog.android.log.Logs
import com.joinforage.datadog.android.log.LogsConfiguration
import com.joinforage.datadog.android.privacy.TrackingConsent
import com.joinforage.forage.android.core.services.EnvConfig
import com.joinforage.forage.android.core.services.ForageConfig

internal abstract class BaseDatadogLoggerFactory(
    private val context: Context,
    private val forageConfig: ForageConfig,
    private val logService: LogService,
    private val logAttrs: LogAttributes,
    private val prefix: String
) {
    private fun getDatadogLogEngine(
        context: Context,
        forageConfig: ForageConfig,
        logService: LogService
    ): Logger {
        val envConfig = EnvConfig.fromForageConfig(forageConfig)
        val configuration = Configuration.Builder(
            clientToken = envConfig.ddClientToken,
            env = envConfig.FLAVOR.value,
            variant = envConfig.FLAVOR.value
        ).build()

        if (!Datadog.isInitialized(FORAGE_DATADOG_INSTANCE_NAME)) {
            Datadog.initialize(FORAGE_DATADOG_INSTANCE_NAME, context, configuration, TrackingConsent.GRANTED)
            val logsConfig = LogsConfiguration.Builder().build()
            Logs.enable(logsConfig)
        }

        return Logger.Builder()
            .setNetworkInfoEnabled(true)
            .setLogcatLogsEnabled(false)
            .setBundleWithTraceEnabled(true)
            .setName("ForageSDK")
            .setService(logService.serviceName)
            .build()
    }

    fun makeLogger(): DatadogLogger {
        val dd = getDatadogLogEngine(context, forageConfig, logService)
        return DatadogLogger(dd, logAttrs, prefix)
    }

    companion object {
        const val FORAGE_DATADOG_INSTANCE_NAME = "com.joinforage"
    }
}
