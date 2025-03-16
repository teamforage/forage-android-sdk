package com.joinforage.forage.android.ecom.services.vault

import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.engine.HttpRequestFailedException
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.network.UnknownErrorApiResponse
import com.joinforage.forage.android.core.services.forageapi.network.UnknownTimeoutErrorResponse
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.IPaymentMethodService
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethodService
import com.joinforage.forage.android.core.services.telemetry.LogLogger
import com.joinforage.forage.android.core.services.vault.errors.IErrorStrategy

internal class TokenizeCardService(
    val logger: LogLogger,
    val forageConfig: ForageConfig,
    val pmService: IPaymentMethodService
) {

    private suspend fun _tokenizeCard(
        cardNumber: String,
        customerId: String?,
        reusable: Boolean
    ): ForageApiResponse<String> {
        val response = pmService.createPaymentMethod(
            rawPan = cardNumber,
            customerId = customerId,
            reusable = reusable
        )
        return ForageApiResponse.Success(response.json)
    }

    suspend fun tokenizeCard(
        cardNumber: String,
        customerId: String?,
        reusable: Boolean
    ): ForageApiResponse<String> = try {
        _tokenizeCard(cardNumber, customerId, reusable)
    } catch (e: Throwable) {
        TokenizationErrorStrategy(logger).handleError(e) { /* Do Nothing ... */ }
    }
}

internal class TokenizationErrorStrategy(private val logLogger: LogLogger) : IErrorStrategy {
    override suspend fun handleError(error: Throwable, cleanup: () -> Unit): ForageApiResponse<String> {
        return when (error) {
            is PaymentMethodService.FailedToCreatePaymentMethodException -> {
                cleanup()
                logLogger.e("[END] Failed to tokenize PaymentMethod.")
                UnknownErrorApiResponse
            }
            is HttpRequestFailedException -> {
                cleanup()
                logLogger.e("[END] Failed to tokenize PaymentMethod.", error.cause)
                when (error.cause) {
                    is java.net.SocketTimeoutException -> UnknownTimeoutErrorResponse
                    else -> UnknownErrorApiResponse
                }
            }
            else -> {
                cleanup()
                logLogger.e("[END] Failed to tokenize PaymentMethod.", error)
                UnknownErrorApiResponse
            }
        }
    }
}
