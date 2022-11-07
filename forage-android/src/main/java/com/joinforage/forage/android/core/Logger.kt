package com.joinforage.forage.android.core

import android.util.Log

internal interface Logger {
    fun debug(msg: String)

    fun info(msg: String)

    fun warning(msg: String)

    fun error(msg: String, t: Throwable? = null)

    companion object {
        private const val TAG = "ForageSDK"

        fun getInstance(enableLogging: Boolean): Logger {
            return if (enableLogging) {
                LOGCAT
            } else {
                SILENT
            }
        }

        private val LOGCAT = object : Logger {
            override fun debug(msg: String) {
                Log.d(TAG, msg)
            }

            override fun info(msg: String) {
                Log.i(TAG, msg)
            }

            override fun warning(msg: String) {
                Log.w(TAG, msg)
            }

            override fun error(msg: String, t: Throwable?) {
                Log.e(TAG, msg, t)
            }
        }

        private val SILENT = object : Logger {
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
