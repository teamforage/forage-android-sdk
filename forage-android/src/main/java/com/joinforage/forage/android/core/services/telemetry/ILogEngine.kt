package com.joinforage.forage.android.core.services.telemetry

internal interface ILogEngine {
    fun captureLog(loggable: Loggable)
}
