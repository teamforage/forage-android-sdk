package com.joinforage.forage.android.network.data

import com.joinforage.forage.android.collect.PinCollector
import com.joinforage.forage.android.core.Logger
import com.joinforage.forage.android.network.CheckBalanceResponseService
import com.joinforage.forage.android.network.EncryptionKeyService
import com.joinforage.forage.android.network.MessageStatusService
import com.joinforage.forage.android.network.model.EncryptionKey
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import com.joinforage.forage.android.network.model.Message
import kotlinx.coroutines.delay

internal class CheckBalanceRepository(
    private val pinCollector: PinCollector,
    private val encryptionKeyService: EncryptionKeyService,
    private val messageStatusService: MessageStatusService,
    private val checkBalanceResponseService: CheckBalanceResponseService,
    private val logger: Logger
) {
    suspend fun checkBalance(
        paymentMethodRef: String,
        cardToken: String
    ): ForageApiResponse<String> {
        return when (val response = encryptionKeyService.getEncryptionKey()) {
            is ForageApiResponse.Success -> collectPinToCheckBalance(
                paymentMethodRef = paymentMethodRef,
                cardToken = cardToken,
                EncryptionKey.ModelMapper.from(response.data).alias
            )
            else -> response
        }
    }

    private suspend fun collectPinToCheckBalance(
        paymentMethodRef: String,
        cardToken: String,
        encryptionKey: String
    ): ForageApiResponse<String> {
        val vgsResponse = pinCollector.collectPinForBalanceCheck(
            paymentMethodRef = paymentMethodRef,
            cardToken = cardToken,
            encryptionKey = encryptionKey
        )

        return when (vgsResponse) {
            is ForageApiResponse.Success -> pollingBalanceMessageStatus(
                contentId = Message.ModelMapper.from(vgsResponse.data).contentId,
                paymentMethodRef = paymentMethodRef
            )
            else -> vgsResponse
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
                            return ForageApiResponse.Failure(error.statusCode, listOf(ForageError(error.statusCode, error.forageCode, error.message)))
                        }
                        break
                    } else {
                        logger.debug("Status is ${balanceMessage.status}.")
                    }

                    if (balanceMessage.failed) {
                        logger.debug("Failed is true.")
                        val error = balanceMessage.errors[0]
                        return ForageApiResponse.Failure(error.statusCode, listOf(ForageError(error.statusCode, error.forageCode, error.message)))
                    }
                }
                else -> {
                    return response
                }
            }

            if (attempt == MAX_ATTEMPTS) {
                logger.debug("Max attempts reached. Returning last response")
                return ForageApiResponse.Failure(500, listOf(ForageError(500, "server_error", "Unknown Server Error")))
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
