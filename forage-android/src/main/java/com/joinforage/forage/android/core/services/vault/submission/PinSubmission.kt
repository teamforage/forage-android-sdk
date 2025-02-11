package com.joinforage.forage.android.core.services.vault.submission

import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.telemetry.LogLogger
import com.joinforage.forage.android.core.services.telemetry.UserAction
import com.joinforage.forage.android.core.services.vault.RosettaPinSubmitter
import com.joinforage.forage.android.core.services.vault.errors.IErrorStrategy
import com.joinforage.forage.android.core.services.vault.metrics.IMetricsRecorder
import com.joinforage.forage.android.core.services.vault.requests.ISubmitRequestBuilder
import java.util.UUID

internal class PinSubmission(
    private val vaultSubmitter: RosettaPinSubmitter,
    private val errorStrategy: IErrorStrategy,
    private val requestBuilder: ISubmitRequestBuilder,
    private val metricsRecorder: IMetricsRecorder,
    private val userAction: UserAction,
    private val logLogger: LogLogger,
    private val traceId: String = logLogger.traceId
) {

    private suspend fun _submit(): ForageApiResponse<String> {
        logLogger.setAction(userAction)
        logLogger.i("[START] Submit Attempt")
        metricsRecorder.startCustomerPerceptionMetric()

        if (!vaultSubmitter.collector.isComplete()) {
            throw UserIncompletePinException()
        }

        val request = requestBuilder.buildRequest(
            "${UUID.randomUUID()}",
            traceId,
            vaultSubmitter
        )

        metricsRecorder.startVaultProxyMetric()
        val response = vaultSubmitter.submit(request)

        logLogger.i("[END] Submission succeeded!")
        metricsRecorder.recordMetrics(response)
        cleanup()

        return response
    }

    private fun cleanup() {
        vaultSubmitter.collector.clearText()
    }

    suspend fun submit(): ForageApiResponse<String> = try {
        _submit()
    } catch (e: Throwable) {
        if (e is RosettaPinSubmitter.VaultForageErrorResponseException) {
            metricsRecorder.recordMetrics(e.failure)
        }
        errorStrategy.handleError(e) { cleanup() }
    }

    class UserIncompletePinException : Exception()
}
