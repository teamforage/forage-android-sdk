package com.joinforage.forage.android.mock

import android.content.Context
import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.telemetry.Log

internal class LogEntry(message: String, attributes: Map<String, Any?>, error: Throwable? = null) {
    private var message = ""
    private var attributes = mapOf<String, Any?>()
    private var error: Throwable? = null

    init {
        this.message = message
        this.attributes = attributes
        this.error = error
    }

    fun getMessage(): String {
        return message
    }

    fun getAttributes(): Map<String, Any?> {
        return attributes
    }

    fun getError(): Throwable? {
        return error
    }
}

internal class MockLogger : Log {
    val infoLogs: MutableList<LogEntry> = mutableListOf()
    val warnLogs: MutableList<LogEntry> = mutableListOf()
    val errorLogs: MutableList<LogEntry> = mutableListOf()

    internal fun getMetricsLog() = infoLogs.last()

    private val cumulativeAttributes = mutableMapOf<String, Any?>()

    override fun initializeDD(context: Context, config: ForageConfig) {
        return
    }

    override fun d(msg: String, attributes: Map<String, Any?>) {
        return
    }

    override fun i(msg: String, attributes: Map<String, Any?>) {
        infoLogs.add(LogEntry(msg, cumulativeAttributes.plus(attributes)))
    }

    override fun w(msg: String, attributes: Map<String, Any?>) {
        warnLogs.add(LogEntry(msg, cumulativeAttributes.plus(attributes)))
    }

    override fun e(msg: String, throwable: Throwable?, attributes: Map<String, Any?>) {
        errorLogs.add(
            LogEntry(
                msg,
                cumulativeAttributes.plus(attributes),
                error = throwable
            )
        )
    }

    override fun addAttribute(key: String, value: Any?): Log {
        cumulativeAttributes[key] = value
        return this
    }

    override fun getTraceIdValue(): String {
        return "11223344"
    }
}
