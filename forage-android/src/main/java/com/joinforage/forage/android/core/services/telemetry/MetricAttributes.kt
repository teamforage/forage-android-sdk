package com.joinforage.forage.android.core.services.telemetry

internal data class MetricAttributes(
    val metricName: MetricName,
    val httpStatus: Int? = null,
    val responseTimeMs: Double? = null,
    val metricOutcome: MetricOutcome? = null,
    val forageErrorCode: String? = null
) : ILoggableAttributes {
    val eventType = LogType.METRIC

    enum class AttributeKey(val key: String) {
        HTTP_STATUS("http_status"),
        RESPONSE_TIME_MS("response_time_ms"),
        EVENT_NAME("event_name"),
        EVENT_OUTCOME("event_outcome"),
        FORAGE_ERROR_CODE("forage_error_code"),
        LOG_TYPE("log_type");
    }

    override fun toMap(): Map<String, String> = mapOf(
        AttributeKey.HTTP_STATUS.key to httpStatus.toString(),
        AttributeKey.RESPONSE_TIME_MS.key to responseTimeMs.toString(),
        AttributeKey.EVENT_NAME.key to metricName.value,
        AttributeKey.EVENT_OUTCOME.key to metricOutcome?.value,
        AttributeKey.FORAGE_ERROR_CODE.key to forageErrorCode,
        AttributeKey.LOG_TYPE.key to eventType.value
    ).filterValues { it != null }
        .mapValues { it.value!! }
}
