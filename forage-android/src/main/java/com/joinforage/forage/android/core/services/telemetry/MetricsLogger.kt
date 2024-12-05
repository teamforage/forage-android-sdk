package com.joinforage.forage.android.core.services.telemetry

import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse

internal open class MetricLogger(
    val logLogger: LogLogger,
    private var metricAttrs: MetricAttributes,
    val startTime: Long = System.nanoTime()
) {

    fun logResult(apiResponse: ForageApiResponse<String>) {
        val end = System.nanoTime()
        val duration = calculateDuration(startTime, end)
        metricAttrs = if (apiResponse is ForageApiResponse.Failure) {
            metricAttrs.copy(
                responseTimeMs = duration,
                forageErrorCode = apiResponse.error.code,
                httpStatus = apiResponse.error.httpStatusCode,
                metricOutcome = MetricOutcome.FAILURE
            )
        } else {
            metricAttrs.copy(
                responseTimeMs = duration,
                httpStatus = 200,
                metricOutcome = MetricOutcome.SUCCESS
            )
        }
        logLogger.m("Outcome recorded!", metricAttrs)
    }

    // Calculate the time in milliseconds between the start and end time
    private fun calculateDuration(startTime: Long, endTime: Long): Double =
        (endTime - startTime).toDouble() / 1000000
}

/*
    VaultProxyResponseMonitor is used to track the response time from the VGS and BT submit
    functions. The timer begins when a balance or capture request is submitted to VGS/BT
    and ends when a response is received by the SDK.
     */
internal class VaultProxyMetricLogger(logEngine: LogLogger) :
    MetricLogger(logEngine, MetricAttributes(MetricName.VAULT_RESPONSE))

/*
    CustomerPerceivedResponseMonitor is used to track the response time that a customer
    experiences while executing a balance or capture action. There are multiple chained requests
    that come from the client when executing a balance or capture action. The timer begins when the
    first HTTP request is sent from the SDK and ends when the the SDK returns information back to
    the user. Ex of a balance action:
    Timer Begins -> [GET] EncryptionKey -> [GET] PaymentMethod -> [POST] to VGS/BT ->
    [GET] Poll for Response -> [GET] PaymentMethod -> Timer Ends -> Return Balance
     */
internal class CustomerPerceivedMetricLogger(logEngine: LogLogger) :
    MetricLogger(logEngine, MetricAttributes(MetricName.CUSTOMER_PERCEIVED_RESPONSE))
