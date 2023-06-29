package com.joinforage.forage.android.core

import android.content.Context
import com.datadog.android.Datadog
import com.datadog.android.core.configuration.Configuration
import com.datadog.android.core.configuration.Credentials
import com.datadog.android.log.Logger
import com.datadog.android.privacy.TrackingConsent
import com.joinforage.forage.android.BuildConfig

internal object DDManager {
    var logger: Logger? = null
    private const val LOGGER_NAME = "ForageSDK"

    internal fun initializeLogger(context: Context): Logger {
        if (logger != null) {
            return logger as Logger
        }
        val configuration = Configuration.Builder(
            logsEnabled = true,
            tracesEnabled = true,
            crashReportsEnabled = true,
            rumEnabled = false
        ).build()
        val credentials = Credentials(
            clientToken = BuildConfig.DD_CLIENT_TOKEN,
            envName = BuildConfig.FLAVOR,
            variant = BuildConfig.FLAVOR,
            rumApplicationId = null,
            serviceName = "android-sdk"
        )
        Datadog.initialize(context, credentials, configuration, TrackingConsent.GRANTED)
        logger = Logger.Builder()
            .setNetworkInfoEnabled(true)
            .setLogcatLogsEnabled(false)
            .setDatadogLogsEnabled(true)
            .setBundleWithTraceEnabled(true)
            .setLoggerName(LOGGER_NAME)
            .build()
        return logger as Logger
    }

    internal fun getLogger(): Logger {
        return logger!!
    }
}
