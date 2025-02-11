package com.joinforage.forage.android.core.services.telemetry

import android.content.Context
import com.joinforage.forage.android.core.services.EnvConfig
import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.generateTraceId

internal class EcomDatadogLoggerFactory(
    context: Context,
    forageConfig: ForageConfig,
    customerId: String?,
    traceId: String = generateTraceId()
) : BaseDatadogLoggerFactory(
    context,
    forageConfig,
    LogService.Ecom,
    LogAttributes(
        forageConfig = forageConfig,
        traceId = traceId,
        customerId = customerId
    ),
    "[${LogService.Ecom.logPrefix}-v${EnvConfig.fromForageConfig(forageConfig).PUBLISH_VERSION}]"
)
