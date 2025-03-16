package com.joinforage.forage.android.core.logger

internal interface LogAttrsContainer {
    val noPM: Map<String, String>
    val all: Map<String, String>
}

internal interface MetricAttributesContainer {
    val vaultRes: Map<String, String>
    val cusPercep: Map<String, String>
}

internal data class LoggableAttributes(
    val logAttrs: LogAttrsContainer,
    val metricAttrs: MetricAttributesContainer
)