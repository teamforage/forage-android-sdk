package com.joinforage.forage.android.ecom.services.vault

import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.engine.ForageErrorResponseException
import com.joinforage.forage.android.core.services.forageapi.engine.HttpRequestFailedException
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.network.UnknownErrorApiResponse
import com.joinforage.forage.android.core.services.forageapi.network.UnknownTimeoutErrorResponse
import com.joinforage.forage.android.core.services.telemetry.LogLogger
import com.joinforage.forage.android.core.services.vault.errors.IErrorStrategy
import com.joinforage.forage.android.ecom.services.forageapi.paymentmethod.IPaymentMethodService
import javax.inject.Inject

internal class TokenizeCardService @Inject constructor(
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

    private suspend fun _tokenizeCreditCard(creditCardParams: CreditCardParams): ForageApiResponse<String> {
        val response = pmService.createCreditPaymentMethod(creditCardParams)
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

    suspend fun tokenizeCreditCard(creditCardParams: CreditCardParams): ForageApiResponse<String> = try {
        _tokenizeCreditCard(creditCardParams)
    } catch (e: Throwable) {
        TokenizationErrorStrategy(logger).handleError(e) { /* Do Nothing ... */ }
    }
}

internal class TokenizationErrorStrategy(private val logLogger: LogLogger) : IErrorStrategy {
    override suspend fun handleError(error: Throwable, cleanup: () -> Unit): ForageApiResponse<String> {
        return when (error) {
            is ForageErrorResponseException -> {
                cleanup()
                logLogger.e("[END] Failed to tokenize PaymentMethod.", error)
                ForageApiResponse.Failure(error.forageError)
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

internal data class CreditCardParams(
    val cardNumber: String,
    val customerId: String? = null,
    val reusable: Boolean = true,
    val name: String,
    val zipCode: String,
    val expiration: Pair<Int, Int>,
    val cvc: String,
    val isHsaFsa: Boolean
)
