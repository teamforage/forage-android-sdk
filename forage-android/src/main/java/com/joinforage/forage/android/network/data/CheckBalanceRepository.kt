package com.joinforage.forage.android.network.data

import com.joinforage.forage.android.collect.PinCollector
import com.joinforage.forage.android.core.telemetry.Log
import com.joinforage.forage.android.model.EncryptionKeys
import com.joinforage.forage.android.model.PaymentMethod
import com.joinforage.forage.android.network.EncryptionKeyService
import com.joinforage.forage.android.network.MessageStatusService
import com.joinforage.forage.android.network.PaymentMethodService
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import com.joinforage.forage.android.network.model.Message
import kotlinx.coroutines.delay

internal class CheckBalanceRepository(
    private val pinCollector: PinCollector,
    private val encryptionKeyService: EncryptionKeyService,
    private val paymentMethodService: PaymentMethodService,
    private val messageStatusService: MessageStatusService,
    private val logger: Log
) {

    suspend fun checkBalance(
        paymentMethodRef: String
    ): ForageApiResponse<String> {
        return when (val response = encryptionKeyService.getEncryptionKey()) {
            is ForageApiResponse.Success -> getTokenFromPaymentMethod(
                paymentMethodRef = paymentMethodRef,
                encryptionKey = pinCollector.parseEncryptionKey(
                    EncryptionKeys.ModelMapper.from(response.data)
                )
            )
            else -> response
        }
    }

    private suspend fun getTokenFromPaymentMethod(
        paymentMethodRef: String,
        encryptionKey: String,
    ): ForageApiResponse<String> {
        return when (val response = paymentMethodService.getPaymentMethod(paymentMethodRef)) {
            is ForageApiResponse.Success -> collectPinToCheckBalance(
                paymentMethodRef = paymentMethodRef,
                cardToken = pinCollector.parseVaultToken(PaymentMethod.ModelMapper.from(response.data)),
                encryptionKey = encryptionKey
            )
            else -> response
        }
    }

    private suspend fun collectPinToCheckBalance(
        paymentMethodRef: String,
        cardToken: String,
        encryptionKey: String
    ): ForageApiResponse<String> {
        val response = pinCollector.collectPinForBalanceCheck(
            paymentMethodRef = paymentMethodRef,
            cardToken = cardToken,
            encryptionKey = encryptionKey
        )

        return when (response) {
            is ForageApiResponse.Success -> pollingBalanceMessageStatus(
                contentId = Message.ModelMapper.from(response.data).contentId,
                paymentMethodRef = paymentMethodRef
            )
            else -> response
        }
    }

    private suspend fun pollingBalanceMessageStatus(
        contentId: String,
        paymentMethodRef: String
    ): ForageApiResponse<String> {
        var attempt = 1

        while (true) {
            logger.i(
                "[HTTP] Polling for balance check response for Payment Method $paymentMethodRef",
                attributes = mapOf(
                    "payment_method_ref" to paymentMethodRef,
                    "content_id" to contentId
                )
            )

            when (val response = messageStatusService.getStatus(contentId)) {
                is ForageApiResponse.Success -> {
                    val balanceMessage = Message.ModelMapper.from(response.data)

                    if (balanceMessage.status == "completed") {
                        if (balanceMessage.failed) {
                            val error = balanceMessage.errors[0]
                            logger.e(
                                "[HTTP] Received response ${error.statusCode} for balance check of Payment Method $paymentMethodRef with message: ${error.message}",
                                attributes = mapOf(
                                    "payment_method_ref" to paymentMethodRef,
                                    "content_id" to contentId
                                )
                            )
                            return ForageApiResponse.Failure(listOf(ForageError(error.statusCode, error.forageCode, error.message)))
                        }
                        break
                    }

                    if (balanceMessage.failed) {
                        val error = balanceMessage.errors[0]
                        logger.e(
                            "[HTTP] Received response ${error.statusCode} for balance check of Payment Method $paymentMethodRef with message: ${error.message}",
                            attributes = mapOf(
                                "payment_method_ref" to paymentMethodRef,
                                "content_id" to contentId
                            )
                        )
                        return ForageApiResponse.Failure(listOf(ForageError(error.statusCode, error.forageCode, error.message)))
                    }
                }
                else -> {
                    return response
                }
            }

            if (attempt == MAX_ATTEMPTS) {
                logger.e(
                    "[HTTP] Max polling attempts reached for balance check of Payment Method $paymentMethodRef",
                    attributes = mapOf(
                        "payment_method_ref" to paymentMethodRef,
                        "content_id" to contentId
                    )
                )
                return ForageApiResponse.Failure(listOf(ForageError(500, "unknown_server_error", "Unknown Server Error")))
            }

            attempt += 1
            delay(POLLING_INTERVAL_IN_MILLIS)
        }

        logger.i(
            "[HTTP] Polling for balance check response succeeded for Payment Method $paymentMethodRef",
            attributes = mapOf(
                "payment_method_ref" to paymentMethodRef,
                "content_id" to contentId
            )
        )

        return when (val response = paymentMethodService.getPaymentMethod(paymentMethodRef)) {
            is ForageApiResponse.Success -> {
                logger.i(
                    "[HTTP] Received updated balance information for Payment Method $paymentMethodRef",
                    attributes = mapOf(
                        "payment_method_ref" to paymentMethodRef,
                        "content_id" to contentId
                    )
                )
                val paymentMethod = PaymentMethod.ModelMapper.from(response.data)
                return ForageApiResponse.Success(paymentMethod.balance.toString())
            }
            else -> response
        }
    }

    companion object {
        private const val POLLING_INTERVAL_IN_MILLIS = 1000L
        private const val MAX_ATTEMPTS = 10

        private fun ForageApiResponse<String>.getStringResponse() = when (this) {
            is ForageApiResponse.Failure -> this.errors[0].message
            is ForageApiResponse.Success -> this.data
        }
    }
}
