package com.joinforage.forage.android.core.services.forageapi.polling

import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.network.ForageError
import com.joinforage.forage.android.core.services.getJitterAmount
import com.joinforage.forage.android.core.services.launchdarkly.LDManager
import com.joinforage.forage.android.core.services.telemetry.Log
import kotlinx.coroutines.delay

internal class PollingService(
    private val messageStatusService: MessageStatusService,
    private val logger: Log
) {
    /**
     * Polls until the message status is completed or failed
     * @param contentId the content id of the message
     * @param operationDescription e.g. "balance check of Payment Method $paymentMethodRef"
     */
    internal suspend fun execute(
        contentId: String,
        operationDescription: String
    ): ForageApiResponse<String> {
        logger.addAttribute("content_id", contentId)

        var attempt = 1
        val pollingIntervals = LDManager.getPollingIntervals(logger)

        while (true) {
            logger.i(
                "[Polling] Start polling Message $contentId to $operationDescription"
            )

            when (val response = messageStatusService.getStatus(contentId)) {
                is ForageApiResponse.Success -> {
                    val sqsMessage = Message.ModelMapper.from(response.data)

                    if (sqsMessage.status == "completed") {
                        if (sqsMessage.failed) {
                            val sqsError = sqsMessage.errors[0]
                            logger.e(
                                "[Polling] Received response ${sqsError.statusCode} for $operationDescription with message: ${sqsError.message}"
                            )
                            return sqsError.toForageError()
                        }
                        break
                    }

                    if (sqsMessage.failed) {
                        val sqsError = sqsMessage.errors[0]
                        logger.e(
                            "[Polling] Received response ${sqsError.statusCode} for $operationDescription with message: ${sqsError.message}"
                        )
                        return sqsError.toForageError()
                    }
                }
                else -> {
                    return response
                }
            }

            if (attempt == MAX_ATTEMPTS) {
                logger.e("[Polling] Max attempts ($MAX_ATTEMPTS) reached for Message $contentId for $operationDescription")

                return ForageApiResponse.Failure(
                    listOf(
                        ForageError(
                            500,
                            "unknown_server_error",
                            "Unknown Server Error"
                        )
                    )
                )
            }

            val index = attempt - 1
            val intervalTime: Long = if (index < pollingIntervals.size) {
                pollingIntervals[index]
            } else {
                1000L
            }

            attempt += 1
            delay(intervalTime + getJitterAmount())
        }

        logger.i("[Polling] Finished polling Message $contentId for $operationDescription")
        return ForageApiResponse.Success("")
    }

    companion object {
        private const val MAX_ATTEMPTS = 10
    }
}
