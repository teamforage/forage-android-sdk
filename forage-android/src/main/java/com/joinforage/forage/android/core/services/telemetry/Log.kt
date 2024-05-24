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
import kotlin.random.Random

internal interface Log {
    fun initializeDD(context: Context, config: ForageConfig)
    fun d(msg: String, attributes: Map<String, Any?> = emptyMap()): Log
    fun i(msg: String, attributes: Map<String, Any?> = emptyMap()): Log
    fun w(msg: String, attributes: Map<String, Any?> = emptyMap()): Log
    fun e(msg: String, throwable: Throwable? = null, attributes: Map<String, Any?> = emptyMap()): Log
    fun getTraceIdValue(): String
    fun addAttribute(key: String, value: Any?): Log

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

            override fun initializeDD(context: Context, config: ForageConfig) {
                if (logger != null) {
                    return
                }
                val envConfig = EnvConfig.fromForageConfig(config)

                val configuration = Configuration.Builder(
                    clientToken = envConfig.ddClientToken,
                    env = envConfig.FLAVOR.value,
                    variant = envConfig.FLAVOR.value
                ).build()
                Datadog.initialize(context, configuration, TrackingConsent.GRANTED)
                val logsConfig = LogsConfiguration.Builder().build()
                Logs.enable(logsConfig)

                logger = Logger.Builder()
                    .setNetworkInfoEnabled(true)
                    .setLogcatLogsEnabled(false)
                    .setBundleWithTraceEnabled(true)
                    .setName(LOGGER_NAME)
                    .setService(SERVICE_NAME)
                    .build()

                if (traceId == null) {
                    traceId = generateTraceId()
                }

                logger?.addAttribute(VERSION_CODE, envConfig.PUBLISH_VERSION)
                logger?.addTag(VERSION_CODE, envConfig.PUBLISH_VERSION)
                logger?.addAttribute(TRACE_ID, traceId)
            }

            override fun d(msg: String, attributes: Map<String, Any?>): Log {
                logger!!.d(msg, attributes = attributes)
                return this
            }

            override fun i(msg: String, attributes: Map<String, Any?>): Log {
                logger!!.i(msg, attributes = attributes)
                return this
            }

            override fun w(msg: String, attributes: Map<String, Any?>): Log {
                logger!!.w(msg, attributes = attributes)
                return this
            }

            override fun e(msg: String, throwable: Throwable?, attributes: Map<String, Any?>): Log {
                logger!!.e(msg, throwable, attributes)
                return this
            }

            override fun getTraceIdValue(): String {
                if (traceId == null) {
                    return ""
                }
                return traceId as String
            }

            override fun addAttribute(key: String, value: Any?): Log {
                logger?.addAttribute(key, value)
                return this
            }
        }

        private val SILENT = object : Log {
            override fun initializeDD(context: Context, config: ForageConfig) {}

            override fun d(msg: String, attributes: Map<String, Any?>): Log = this

            override fun i(msg: String, attributes: Map<String, Any?>): Log = this

            override fun w(msg: String, attributes: Map<String, Any?>): Log = this

            override fun e(msg: String, throwable: Throwable?, attributes: Map<String, Any?>): Log = this

            override fun getTraceIdValue(): String { return "" }

            override fun addAttribute(key: String, value: Any?) = this
        }

        private fun generateTraceId(): String {
            // Seed the random number generator with current time
            val random = Random(System.currentTimeMillis())
            val length = 14
            return "44" + (1..length).map { random.nextInt(10) }.joinToString("")
        }
    }
}
