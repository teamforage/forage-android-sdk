package com.joinforage.forage.android.core.services.vault.submission

import com.joinforage.forage.android.core.services.UserAction
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.IPaymentMethodService
import com.joinforage.forage.android.core.services.telemetry.LogLogger
import com.joinforage.forage.android.core.services.vault.IPmRefProvider
import com.joinforage.forage.android.core.services.vault.requests.ISubmitRequestBuilder
import com.joinforage.forage.android.core.services.vault.RosettaPinSubmitter
import com.joinforage.forage.android.core.services.vault.errors.IErrorStrategy
import com.joinforage.forage.android.core.services.vault.metrics.IMetricsRecorder
import java.util.UUID

internal class PinSubmission(
    private val vaultSubmitter: RosettaPinSubmitter,
    private val errorStrategy: IErrorStrategy,
    private val requestBuilder: ISubmitRequestBuilder,
    private val metricsRecorder: IMetricsRecorder,
    private val paymentMethodService: IPaymentMethodService,
    private val userAction: UserAction,
    private val logLogger: LogLogger,
    private val traceId: String = logLogger.traceId
) {

    private suspend fun _submit(
        paymentMethodRefProvider: IPmRefProvider
    ): ForageApiResponse<String> {
        logLogger.setAction(userAction)
        logLogger.i("[START] Submit Attempt")
        metricsRecorder.startCustomerPerceptionMetric()

        val paymentMethodRef = paymentMethodRefProvider.getPaymentMethodRef()
        logLogger.setPaymentMethodRef(paymentMethodRef)

        if (!vaultSubmitter.collector.isComplete()) {
            throw UserIncompletePinException()
        }

        val paymentMethod = paymentMethodService.fetchPaymentMethod(paymentMethodRef)

        val request = requestBuilder.buildRequest(
            paymentMethod,
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

    suspend fun submit(
        paymentMethodRefProvider: IPmRefProvider
    ): ForageApiResponse<String> = try {
        _submit(paymentMethodRefProvider)
    } catch (e: Throwable) {
        if (e is RosettaPinSubmitter.VaultForageErrorResponseException) {
            metricsRecorder.recordMetrics(e.failure)
        }
        errorStrategy.handleError(e) { cleanup() }
    }

    class UserIncompletePinException : Exception()
}
