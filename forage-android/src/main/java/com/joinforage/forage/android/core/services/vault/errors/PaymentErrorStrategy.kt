package com.joinforage.forage.android.core.services.vault.errors

import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.network.PaymentErrorResponse
import com.joinforage.forage.android.core.services.forageapi.payment.PaymentService
import com.joinforage.forage.android.core.services.telemetry.LogLogger

internal class PaymentErrorStrategy(
    private val logLogger: LogLogger,
    private val baseErrorStrategy: IErrorStrategy
) : IErrorStrategy {
    override suspend fun handleError(error: Throwable, cleanup: () -> Unit): ForageApiResponse<String> {
        return when (error) {
            is PaymentService.FailedToFetchPaymentException -> {
                cleanup()
                logLogger.e("[END] Submission failed.\n\nFailed to fetch Payment ${error.paymentRef}", error.cause)
                PaymentErrorResponse(error.paymentRef)
            }
            else -> baseErrorStrategy.handleError(error, cleanup)
        }
    }
}
