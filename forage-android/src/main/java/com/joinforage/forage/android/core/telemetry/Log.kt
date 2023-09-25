package com.joinforage.forage.android.core.telemetry

import android.content.Context
import com.datadog.android.Datadog
import com.datadog.android.core.configuration.Configuration
import com.datadog.android.core.configuration.Credentials
import com.datadog.android.log.Logger
import com.datadog.android.privacy.TrackingConsent
import com.joinforage.forage.android.BuildConfig
import kotlin.random.Random

internal interface Log {
    fun initializeDD(context: Context)
    fun d(msg: String, attributes: Map<String, Any?> = emptyMap())
    fun i(msg: String, attributes: Map<String, Any?> = emptyMap())
    fun w(msg: String, attributes: Map<String, Any?> = emptyMap())
    fun e(msg: String, throwable: Throwable? = null, attributes: Map<String, Any?> = emptyMap())
    fun getTraceIdValue(): String
    companion object {
        private const val LOGGER_NAME = "ForageSDK"
        private const val SERVICE_NAME = "android-sdk"
        private const val VERSION_CODE = "version_code"
        private const val TRACE_ID = "trace_id"

        fun getInstance(): Log {
            return LIVE
        }

        fun getSilentInstance(): Log {
            return SILENT
        }

        private val LIVE = object : Log {
            var traceId: String? = null
            private var logger: Logger? = null

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
                    clientToken = StopgapGlobalState.envConfig.ddClientToken,
                    envName = StopgapGlobalState.envConfig.FLAVOR.value,
                    variant = StopgapGlobalState.envConfig.FLAVOR.value,
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

                if (traceId == null) {
                    traceId = generateTraceId()
                }

                logger?.addAttribute(VERSION_CODE, BuildConfig.VERSION)
                logger?.addTag(VERSION_CODE, BuildConfig.VERSION)
                logger?.addAttribute(TRACE_ID, traceId)
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

            override fun getTraceIdValue(): String {
                if (traceId == null) {
                    return ""
                }
                return traceId as String
            }
        }

        private val SILENT = object : Log {
            override fun initializeDD(context: Context) {
            }

            override fun d(msg: String, attributes: Map<String, Any?>) {
            }

            override fun i(msg: String, attributes: Map<String, Any?>) {
            }

            override fun w(msg: String, attributes: Map<String, Any?>) {
            }

            override fun e(msg: String, throwable: Throwable?, attributes: Map<String, Any?>) {
            }

            override fun getTraceIdValue(): String {
                return ""
            }
        }

        private fun generateTraceId(): String {
            // Seed the random number generator with current time
            val random = Random(System.currentTimeMillis())
            val length = 14
            return "44" + (1..length).map { random.nextInt(10) }.joinToString("")
        }
    }
}
