package com.joinforage.forage.android.core

import android.content.Context
import com.datadog.android.Datadog
import com.datadog.android.core.configuration.Configuration
import com.datadog.android.core.configuration.Credentials
import com.datadog.android.log.Logger
import com.datadog.android.privacy.TrackingConsent
import com.joinforage.forage.android.BuildConfig
import io.sentry.android.core.SentryAndroid

internal interface Log {
    fun initializeDD(context: Context)
    fun initializeSentry(context: Context)
    fun d(msg: String, attributes: Map<String, Any?> = emptyMap())
    fun i(msg: String, attributes: Map<String, Any?> = emptyMap())
    fun w(msg: String, attributes: Map<String, Any?> = emptyMap())
    fun e(msg: String, throwable: Throwable? = null, attributes: Map<String, Any?> = emptyMap())
    companion object {
        private const val LOGGER_NAME = "ForageSDK"
        private const val SERVICE_NAME = "android-sdk"
        fun getInstance(): Log {
            return LIVE
        }

        fun getSilentInstance(): Log {
            return SILENT
        }

        private val LIVE = object : Log {
            var logger: Logger? = null

            override fun initializeDD(context: Context) {
                if (logger != null) {
                    return
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
                    serviceName = SERVICE_NAME
                )
                Datadog.initialize(context, credentials, configuration, TrackingConsent.GRANTED)
                logger = Logger.Builder()
                    .setNetworkInfoEnabled(true)
                    .setLogcatLogsEnabled(false)
                    .setDatadogLogsEnabled(true)
                    .setBundleWithTraceEnabled(true)
                    .setLoggerName(LOGGER_NAME)
                    .build()
            }

            override fun initializeSentry(context: Context) {
                val flavor = BuildConfig.FLAVOR
                SentryAndroid.init(context) { options ->
                    options.dsn = "https://1f24f0685c867a440eca683770d4666e@o921422.ingest.sentry.io/4505704106688512"
                    options.tracesSampleRate = 1.0
                    options.environment = flavor
                }
            }

            override fun d(msg: String, attributes: Map<String, Any?>) {
                logger?.d(msg, attributes = attributes)
            }

            override fun i(msg: String, attributes: Map<String, Any?>) {
                logger?.i(msg, attributes = attributes)
            }

            override fun w(msg: String, attributes: Map<String, Any?>) {
                logger?.w(msg, attributes = attributes)
            }

            override fun e(msg: String, throwable: Throwable?, attributes: Map<String, Any?>) {
                logger?.e(msg, throwable, attributes)
            }
        }

        private val SILENT = object : Log {
            override fun initializeDD(context: Context) {
            }

            override fun initializeSentry(context: Context) {
            }

            override fun d(msg: String, attributes: Map<String, Any?>) {
            }

            override fun i(msg: String, attributes: Map<String, Any?>) {
            }

            override fun w(msg: String, attributes: Map<String, Any?>) {
            }

            override fun e(msg: String, throwable: Throwable?, attributes: Map<String, Any?>) {
            }
        }
    }
}
