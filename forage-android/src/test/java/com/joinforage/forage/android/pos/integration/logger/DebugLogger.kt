package com.joinforage.forage.android.pos.integration.logger

import com.joinforage.forage.android.core.services.telemetry.LogAttributes
import com.joinforage.forage.android.core.services.telemetry.Loggable

internal class DebugLogEngine : InMemoryLogEngine() {
    override fun captureLog(loggable: Loggable) {
        println(loggable)
        super.captureLog(loggable)
    }
}

/**
 * Use this logger only for local debugging. We don't want to
 * print a bunch of logs during an automated CI test!!
 */
internal class DebugLogger(logAttrs: LogAttributes) :
    InMemoryLogger(logAttrs, DebugLogEngine())
