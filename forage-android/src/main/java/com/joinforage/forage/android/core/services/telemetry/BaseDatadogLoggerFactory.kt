package com.joinforage.forage.android.core.services.telemetry

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.joinforage.datadog.android.Datadog
import com.joinforage.datadog.android.api.SdkCore
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
    private fun getDatadogLogEngine(): Logger {
        val sdkCore = getSdkCore()
        return Logger.Builder(sdkCore)
            .setNetworkInfoEnabled(true)
            .setLogcatLogsEnabled(false)
            .setBundleWithTraceEnabled(true)
            .setName("ForageSDK")
            .setService(logService.serviceName)
            .build()
    }

    fun makeLogger(): DatadogLogger {
        val dd = getDatadogLogEngine()
        return DatadogLogger(dd, logAttrs, prefix)
    }

    @VisibleForTesting
    internal fun getSdkCore(): SdkCore {
        return synchronized(this::class.java) {
            if (Datadog.isInitialized(FORAGE_DATADOG_INSTANCE_NAME)) {
                Datadog.getInstance(FORAGE_DATADOG_INSTANCE_NAME)
            } else {
                val envConfig = EnvConfig.fromForageConfig(forageConfig)
                val configuration = Configuration.Builder(
                    clientToken = envConfig.ddClientToken,
                    env = envConfig.FLAVOR.value,
                    variant = envConfig.FLAVOR.value
                ).build()

                val sdkCore =
                    Datadog.initialize(FORAGE_DATADOG_INSTANCE_NAME, context, configuration, TrackingConsent.GRANTED)
                        ?: Datadog.getInstance(NO_OP_INSTANCE_NAME)

                val logsConfig = LogsConfiguration.Builder().build()
                Logs.enable(logsConfig, sdkCore)

                sdkCore
            }
        }
    }

    companion object {
        const val FORAGE_DATADOG_INSTANCE_NAME = "com.joinforage"
        const val NO_OP_INSTANCE_NAME = "no_such_instance"
    }
}
