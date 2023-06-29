package com.joinforage.forage.android.core

import com.datadog.android.log.Logger

internal interface Log {
    fun debug(msg: String)

    fun info(msg: String)

    fun warning(msg: String)

    fun error(msg: String, t: Throwable? = null)

    companion object {
        private const val LOGGER_NAME = "ForageSDK"

        fun getInstance(enableLogging: Boolean): Log {
            return if (enableLogging) {
                LOGCAT
            } else {
                SILENT
            }
        }

        private val LOGCAT = object : Log {
            val logger: Logger?
            init {
                logger = Logger.Builder()
                    .setNetworkInfoEnabled(true)
                    .setLogcatLogsEnabled(true)
                    .setDatadogLogsEnabled(true)
                    .setBundleWithTraceEnabled(true)
                    .setLoggerName(LOGGER_NAME)
                    .build()
            }

            override fun debug(msg: String) {
                logger?.d(msg)
            }

            override fun info(msg: String) {
                logger?.i(msg)
            }

            override fun warning(msg: String) {
                logger?.w(msg)
            }

            override fun error(msg: String, t: Throwable?) {
                logger?.e(msg, throwable = t)
            }
        }

        private val SILENT = object : Log {
            override fun debug(msg: String) {
            }

            override fun info(msg: String) {
            }

            override fun warning(msg: String) {
            }

            override fun error(msg: String, t: Throwable?) {
            }
        }
    }
}
