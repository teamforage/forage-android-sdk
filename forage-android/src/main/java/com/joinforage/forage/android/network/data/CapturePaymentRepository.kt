package com.joinforage.forage.android.network.data

import com.joinforage.forage.android.collect.PinCollector
import com.joinforage.forage.android.core.Log
import com.joinforage.forage.android.model.EncryptionKeys
import com.joinforage.forage.android.model.Payment
import com.joinforage.forage.android.model.PaymentMethod
import com.joinforage.forage.android.network.EncryptionKeyService
import com.joinforage.forage.android.network.MessageStatusService
import com.joinforage.forage.android.network.PaymentMethodService
import com.joinforage.forage.android.network.PaymentService
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import com.joinforage.forage.android.network.model.Message
import kotlinx.coroutines.delay

internal class CapturePaymentRepository(
    private val pinCollector: PinCollector,
    private val encryptionKeyService: EncryptionKeyService,
    private val messageStatusService: MessageStatusService,
    private val paymentService: PaymentService,
    private val paymentMethodService: PaymentMethodService,
    private val logger: Log
) {
    suspend fun capturePayment(paymentRef: String): ForageApiResponse<String> {
        return when (val response = encryptionKeyService.getEncryptionKey()) {
            is ForageApiResponse.Success -> getPaymentMethodFromPayment(
                paymentRef = paymentRef,
                pinCollector.parseEncryptionKey(
                    EncryptionKeys.ModelMapper.from(response.data)
                )
            )
            else -> response
        }
    }

    private suspend fun getPaymentMethodFromPayment(
        paymentRef: String,
        encryptionKey: String
    ): ForageApiResponse<String> {
        return when (val response = paymentService.getPayment(paymentRef)) {
            is ForageApiResponse.Success -> getTokenFromPaymentMethod(
                paymentRef = paymentRef,
                paymentMethodRef = Payment.ModelMapper.from(response.data).paymentMethod,
                encryptionKey = encryptionKey
            )
            else -> response
        }
    }

    private suspend fun getTokenFromPaymentMethod(
        paymentRef: String,
        paymentMethodRef: String,
        encryptionKey: String
    ): ForageApiResponse<String> {
        return when (val response = paymentMethodService.getPaymentMethod(paymentMethodRef)) {
            is ForageApiResponse.Success -> collectPinToCapturePayment(
                paymentRef = paymentRef,
                cardToken = pinCollector.parseVaultToken(PaymentMethod.ModelMapper.from(response.data)),
                encryptionKey = encryptionKey
            )
            else -> response
        }
    }

    private suspend fun collectPinToCapturePayment(
        paymentRef: String,
        cardToken: String,
        encryptionKey: String

    ): ForageApiResponse<String> {
        val response = pinCollector.collectPinForCapturePayment(
            paymentRef = paymentRef,
            cardToken = cardToken,
            encryptionKey = encryptionKey
        )

        return when (response) {
            is ForageApiResponse.Success -> {
                pollingCapturePaymentMessageStatus(
                    Message.ModelMapper.from(response.data).contentId,
                    paymentRef
                )
            }
            else -> response
        }
    }

    private suspend fun pollingCapturePaymentMessageStatus(
        contentId: String,
        paymentRef: String
    ): ForageApiResponse<String> {
        var attempt = 1

        while (true) {
            logger.i(
                "Polling for balance check response for Payment $paymentRef",
                attributes = mapOf(
                    "payment_ref" to paymentRef,
                    "content_id" to contentId
                )
            )

            when (val response = messageStatusService.getStatus(contentId)) {
                is ForageApiResponse.Success -> {
                    val paymentMessage = Message.ModelMapper.from(response.data)

                    if (paymentMessage.status == "completed") {
                        if (paymentMessage.failed) {
                            val error = paymentMessage.errors[0]
                            logger.e(
                                "Received response ${error.statusCode} for capture request of Payment $paymentRef with message: ${error.message}",
                                attributes = mapOf(
                                    "payment_ref" to paymentRef,
                                    "content_id" to contentId
                                )
                            )
                            return ForageApiResponse.Failure(listOf(ForageError(error.statusCode, error.forageCode, error.message)))
                        }
                        break
                    }

                    if (paymentMessage.failed) {
                        val error = paymentMessage.errors[0]
                        logger.e(
                            "Received response ${error.statusCode} for capture request of Payment $paymentRef with message: ${error.message}",
                            attributes = mapOf(
                                "payment_ref" to paymentRef,
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
                    "Max polling attempts reached for capture request of Payment $paymentRef",
                    attributes = mapOf(
                        "payment_ref" to paymentRef,
                        "content_id" to contentId
                    )
                )
                return ForageApiResponse.Failure(listOf(ForageError(500, "unknown_server_error", "Unknown Server Error")))
            }

            attempt += 1
            delay(POLLING_INTERVAL_IN_MILLIS)
        }

        logger.i(
            "Polling for capture request response succeeded for Payment $paymentRef",
            attributes = mapOf(
                "payment_ref" to paymentRef,
                "content_id" to contentId
            )
        )

        return paymentService.getPayment(paymentRef = paymentRef)
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
