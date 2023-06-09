package com.joinforage.forage.android.network.data

import com.joinforage.forage.android.collect.PinCollector
import com.joinforage.forage.android.core.Logger
import com.joinforage.forage.android.network.CheckBalanceResponseService
import com.joinforage.forage.android.network.EncryptionKeyService
import com.joinforage.forage.android.network.MessageStatusService
import com.joinforage.forage.android.network.PaymentMethodService
import com.joinforage.forage.android.network.model.EncryptionKey
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import com.joinforage.forage.android.network.model.Message
import com.joinforage.forage.android.network.model.PaymentMethod
import kotlinx.coroutines.delay

internal class CheckBalanceRepository(
    private val pinCollector: PinCollector,
    private val encryptionKeyService: EncryptionKeyService,
    private val paymentMethodService: PaymentMethodService,
    private val messageStatusService: MessageStatusService,
    private val checkBalanceResponseService: CheckBalanceResponseService,
    private val logger: Logger
) {
    suspend fun checkBalance(
        paymentMethodRef: String,
        merchantAccount: String
    ): ForageApiResponse<String> {
        return when (val response = encryptionKeyService.getEncryptionKey()) {
            is ForageApiResponse.Success -> getTokenFromPaymentMethod(
                paymentMethodRef = paymentMethodRef,
                encryptionKey = EncryptionKey.ModelMapper.from(response.data).alias,
                merchantAccount = merchantAccount
            )
            else -> response
        }
    }

    private suspend fun getTokenFromPaymentMethod(
        paymentMethodRef: String,
        encryptionKey: String,
        merchantAccount: String
    ): ForageApiResponse<String> {
        return when (val response = paymentMethodService.getPaymentMethod(paymentMethodRef)) {
            is ForageApiResponse.Success -> collectPinToCheckBalance(
                paymentMethodRef = paymentMethodRef,
                // TODO: Parse the token to get BT or VGS
                cardToken = PaymentMethod.ModelMapper.from(response.data).card?.token ?: "",
                encryptionKey = encryptionKey,
                merchantAccount = merchantAccount
            )
            else -> response
        }
    }

    private suspend fun collectPinToCheckBalance(
        paymentMethodRef: String,
        cardToken: String,
        encryptionKey: String,
        merchantAccount: String
    ): ForageApiResponse<String> {
//        val parsedToken = cardToken.split(",")[1]
        val response = pinCollector.collectPinForBalanceCheck(
            paymentMethodRef = paymentMethodRef,
            cardToken = cardToken,
            encryptionKey = encryptionKey,
            merchantAccount = merchantAccount
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
            logger.debug("Polling check balance message status. Attempt: $attempt.")

            val response = messageStatusService.getStatus(contentId)
            when (response) {
                is ForageApiResponse.Success -> {
                    val balanceMessage = Message.ModelMapper.from(response.data)

                    if (balanceMessage.status == "completed") {
                        logger.debug("Status is completed.")
                        if (balanceMessage.failed) {
                            logger.debug("Failed is true.")
                            val error = balanceMessage.errors[0]
                            return ForageApiResponse.Failure(listOf(ForageError(error.statusCode, error.forageCode, error.message)))
                        }
                        break
                    } else {
                        logger.debug("Status is ${balanceMessage.status}.")
                    }

                    if (balanceMessage.failed) {
                        logger.debug("Failed is true.")
                        val error = balanceMessage.errors[0]
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

        return checkBalanceResponseService.retrieveBalanceResponse(paymentMethodRef = paymentMethodRef)
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
