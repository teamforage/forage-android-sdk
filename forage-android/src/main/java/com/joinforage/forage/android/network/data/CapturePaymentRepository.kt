package com.joinforage.forage.android.network.data

import com.joinforage.forage.android.collect.PinCollector
import com.joinforage.forage.android.core.Logger
import com.joinforage.forage.android.model.EncryptionKey
import com.joinforage.forage.android.model.Payment
import com.joinforage.forage.android.network.EncryptionKeyService
import com.joinforage.forage.android.network.MessageStatusService
import com.joinforage.forage.android.network.PaymentMethodService
import com.joinforage.forage.android.network.PaymentService
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import com.joinforage.forage.android.network.model.Message
import com.joinforage.forage.android.network.model.PaymentMethod
import kotlinx.coroutines.delay

internal class CapturePaymentRepository(
    private val pinCollector: PinCollector,
    private val encryptionKeyService: EncryptionKeyService,
    private val messageStatusService: MessageStatusService,
    private val paymentService: PaymentService,
    private val paymentMethodService: PaymentMethodService,
    private val logger: Logger
) {
    suspend fun capturePayment(paymentRef: String): ForageApiResponse<String> {
        return when (val response = encryptionKeyService.getEncryptionKey()) {
            is ForageApiResponse.Success -> getPaymentMethodFromPayment(
                paymentRef = paymentRef,
                EncryptionKey.ModelMapper.from(response.data).alias
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
                cardToken = PaymentMethod.ModelMapper.from(response.data).card?.token ?: "",
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
        val vgsResponse = pinCollector.collectPinForCapturePayment(
            paymentRef = paymentRef,
            cardToken = cardToken,
            encryptionKey = encryptionKey
        )

        return when (vgsResponse) {
            is ForageApiResponse.Success -> {
                pollingCapturePaymentMessageStatus(
                    Message.ModelMapper.from(vgsResponse.data).contentId,
                    paymentRef
                )
            }
            else -> vgsResponse
        }
    }

    private suspend fun pollingCapturePaymentMessageStatus(
        contentId: String,
        paymentRef: String
    ): ForageApiResponse<String> {
        var attempt = 1

        while (true) {
            logger.debug("Polling capture payment message status. Attempt: $attempt.")

            val response = messageStatusService.getStatus(contentId)
            when (response) {
                is ForageApiResponse.Success -> {
                    val paymentMessage = Message.ModelMapper.from(response.data)

                    if (paymentMessage.status == "completed") {
                        logger.debug("Status is completed.")
                        if (paymentMessage.failed) {
                            logger.debug("Failed is true.")
                            val error = paymentMessage.errors[0]
                            return ForageApiResponse.Failure(listOf(ForageError(error.statusCode, error.forageCode, error.message)))
                        }
                        break
                    } else {
                        logger.debug("Status is ${paymentMessage.status}.")
                    }

                    if (paymentMessage.failed) {
                        logger.debug("Failed is true.")
                        val error = paymentMessage.errors[0]
                        return ForageApiResponse.Failure(listOf(ForageError(error.statusCode, error.forageCode, error.message)))
                    }
                }
                else -> {
                    return response
                }
            }

            if (attempt == MAX_ATTEMPTS) {
                logger.debug("Max attempts reached. Returning last response")
                return ForageApiResponse.Failure(listOf(ForageError(500, "unknown_server_error", "Unknown Server Error")))
            }

            attempt += 1
            delay(POLLING_INTERVAL_IN_MILLIS)
        }

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
