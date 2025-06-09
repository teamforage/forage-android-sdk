package com.joinforage.forage.android.core.services.vault.errors

import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.network.PaymentMethodErrorResponse
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.FetchPaymentMethodService
import com.joinforage.forage.android.core.services.telemetry.LogLogger

internal class PaymentMethodErrorStrategy(
    private val logLogger: LogLogger,
    private val baseErrorStrategy: IErrorStrategy
) : IErrorStrategy {
    override suspend fun handleError(error: Throwable, cleanup: () -> Unit): ForageApiResponse<String> {
        return when (error) {
            is FetchPaymentMethodService.FailedToFetchPaymentMethodException -> {
                cleanup()
                logLogger.e("[END] Submission failed.\n\nFailed to fetch PaymentMethod ${error.paymentMethodRef}", error.cause)
                PaymentMethodErrorResponse(error.paymentMethodRef)
            }
            else -> baseErrorStrategy.handleError(error, cleanup)
        }
    }
}
