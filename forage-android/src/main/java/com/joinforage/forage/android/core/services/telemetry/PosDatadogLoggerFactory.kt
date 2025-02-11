package com.joinforage.forage.android.core.services.telemetry

import android.content.Context
import com.joinforage.forage.android.core.services.EnvConfig
import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.generateTraceId

internal class PosDatadogLoggerFactory(
    context: Context,
    forageConfig: ForageConfig,
    posTerminalId: String,
    traceId: String = generateTraceId()
) : BaseDatadogLoggerFactory(
    context,
    forageConfig,
    LogService.POS,
    LogAttributes(
        forageConfig = forageConfig,
        traceId = traceId,
        posTerminalId = posTerminalId
    ),
    "[${LogService.POS.logPrefix}-v${EnvConfig.fromForageConfig(forageConfig).PUBLISH_VERSION}]"
)
