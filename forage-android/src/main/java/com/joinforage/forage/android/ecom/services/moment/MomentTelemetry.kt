package com.joinforage.forage.android.ecom.services.moment

import com.joinforage.forage.android.core.services.telemetry.LogLogger
import com.joinforage.forage.android.core.services.telemetry.MetricAttributes
import com.joinforage.forage.android.core.services.telemetry.MetricName
import com.joinforage.forage.android.core.services.telemetry.MetricOutcome
import com.joinforage.forage.android.core.services.telemetry.UserAction

/**
 * Logs one moment's lifecycle to Datadog. The iOS SDK sends the same `moment_ready` and
 * `moment_outcome` metrics with the same attributes, so one dashboard covers both: when it's called, when the page is ready, and how it
 * ended. Times are measured from when createMoment was called.
 *
 * Only logs the moment's name, never its params or the session token.
 *
 * @param logger `null` if a logger couldn't be created, in which case nothing is logged
 * @param clock The current time in milliseconds. Injected for tests.
 */
internal class MomentTelemetry(
    private val logger: LogLogger?,
    private val momentName: String,
    private val clock: () -> Long = { System.nanoTime() / 1_000_000 }
) {
    private val startMs = clock()

    init {
        logger?.setAction(UserAction.CREATE_MOMENT)
    }

    private fun elapsedMs() = (clock() - startMs).toDouble()

    fun called() {
        logger?.i("[Moment] Called createMoment for $momentName")
    }

    fun ready() {
        val elapsed = elapsedMs()
        logger?.m(
            "[Moment] $momentName ready after ${elapsed.toLong()}ms",
            MetricAttributes(
                metricName = MetricName.MOMENT_READY,
                responseTimeMs = elapsed,
                momentName = momentName
            )
        )
    }

    fun settled(result: MomentResult, reachedReady: Boolean) {
        val logger = logger ?: return
        val elapsed = elapsedMs()
        val attrs = MetricAttributes(
            metricName = MetricName.MOMENT_OUTCOME,
            responseTimeMs = elapsed,
            momentName = momentName,
            momentReachedReady = reachedReady
        )
        val summary = "$momentName after ${elapsed.toLong()}ms (reached ready: $reachedReady)"

        when (result) {
            MomentResult.Completed -> {
                logger.i("[Moment] Completed $summary")
                logger.m("[Moment] Outcome recorded", attrs.copy(metricOutcome = MetricOutcome.SUCCESS))
            }
            MomentResult.Dismissed -> {
                logger.i("[Moment] Dismissed $summary")
                logger.m("[Moment] Outcome recorded", attrs.copy(metricOutcome = MetricOutcome.DISMISSED))
            }
            is MomentResult.Failed -> {
                // the code and message tell failures apart, e.g. timeout_error for the load
                // deadline and the WebView's render process going away
                val error = result.error
                logger.e("[Moment] Failed $summary: ${error.code} (${error.httpStatusCode}) ${error.message}")
                logger.m(
                    "[Moment] Outcome recorded",
                    attrs.copy(
                        httpStatus = error.httpStatusCode,
                        forageErrorCode = error.code,
                        metricOutcome = MetricOutcome.FAILURE
                    )
                )
            }
        }
    }
}
