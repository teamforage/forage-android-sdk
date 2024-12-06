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
import com.joinforage.forage.android.core.services.generateTraceId
import com.joinforage.forage.android.pos.services.vault.rosetta.AndroidBase64Util

internal class DatadogEngine(val dd: Logger) : ILogEngine {
    override fun captureLog(loggable: Loggable) {
        val msg = loggable.toString()
        when (loggable) {
            is Loggable.Debug -> dd.d(msg, attributes = loggable.attrs)
            is Loggable.Info -> dd.i(msg, attributes = loggable.attrs)
            is Loggable.Metric -> dd.i(msg, attributes = loggable.attrs)
            is Loggable.Warn -> dd.w(msg, attributes = loggable.attrs)
            is Loggable.Error -> dd.e(msg, loggable.throwable, attributes = loggable.attrs)
        }
    }
}

internal class DatadogLogger(
    val dd: Logger,
    logAttrs: LogAttributes,
    prefix: String
) : LogLogger(
    DatadogEngine(dd),
    AndroidBase64Util(),
    logAttrs,
    prefix
) {

    init {
        dd.addTag(LogAttributes.AttributesKey.VERSION_CODE.key, logAttrs.versionCode)
    }

    override fun setAction(action: UserAction) {
        // we go out of our way to make set the user action tag
        // to help gather insights in Datadog
        dd.addTag(LogAttributes.AttributesKey.ACTION.key, action.value)
        super.setAction(action)
    }

    companion object {

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

            if (!Datadog.isInitialized()) {
                Datadog.initialize(context, configuration, TrackingConsent.GRANTED)
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

        fun getPosDatadogInstance(
            context: Context,
            forageConfig: ForageConfig
        ) = getDatadogLogEngine(context, forageConfig, LogService.POS)

        fun getEcomDatadogInstance(
            context: Context,
            forageConfig: ForageConfig
        ) = getDatadogLogEngine(context, forageConfig, LogService.Ecom)

        fun forEcom(
            ddLogger: Logger,
            forageConfig: ForageConfig,
            traceId: String = generateTraceId()
        ): DatadogLogger {
            val attrs = LogAttributes(forageConfig = forageConfig, traceId = traceId)
            return DatadogLogger(
                ddLogger,
                attrs,
                "[${LogService.Ecom.logPrefix}-v${attrs.versionCode}]"
            )
        }

        fun forPos(
            ddLogger: Logger,
            forageConfig: ForageConfig,
            posTerminalId: String,
            traceId: String = generateTraceId()
        ): DatadogLogger {
            val attrs = LogAttributes(
                forageConfig = forageConfig,
                traceId = traceId,
                posTerminalId = posTerminalId
            )
            return DatadogLogger(
                ddLogger,
                attrs,
                "[${LogService.POS.logPrefix}-v${attrs.versionCode}]"
            )
        }
    }
}
