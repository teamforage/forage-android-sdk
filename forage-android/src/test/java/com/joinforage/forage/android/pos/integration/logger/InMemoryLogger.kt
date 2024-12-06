package com.joinforage.forage.android.pos.integration.logger

import com.joinforage.forage.android.core.services.telemetry.ILogEngine
import com.joinforage.forage.android.core.services.telemetry.LogAttributes
import com.joinforage.forage.android.core.services.telemetry.LogLogger
import com.joinforage.forage.android.core.services.telemetry.Loggable
import com.joinforage.forage.android.pos.integration.base64.JavaBase64Util

internal open class InMemoryLogEngine : ILogEngine {
    val logs: MutableList<Loggable> = emptyList<Loggable>().toMutableList()

    override fun captureLog(loggable: Loggable) {
        logs.add(loggable)
    }
}

/**
 * This is the typical logger to use for automated testing.
 * Use a DebugLogger for cases where you want to see
 * printed logs
 */
internal open class InMemoryLogger(
    logAttrs: LogAttributes,
    override val logEngine: InMemoryLogEngine = InMemoryLogEngine()
) : LogLogger(
    logEngine,
    JavaBase64Util(),
    logAttrs
) {
    val logs: List<Loggable>
        get() = logEngine.logs
}
