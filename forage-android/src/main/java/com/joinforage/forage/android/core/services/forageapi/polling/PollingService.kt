package com.joinforage.forage.android.core.services.forageapi.polling

import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.network.ForageError
import com.joinforage.forage.android.core.services.getJitterAmount
import com.joinforage.forage.android.core.services.telemetry.Log
import kotlinx.coroutines.delay

// we'll be nuking the whole concept of polling soon
// this variable was introduced to break the dependence
// on Launch Darkly so LD can be moved to Ecom
// from core since Pos does not use LD
internal val TEMPOARAY_polling_intervals = longArrayOf(1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000)

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
        val pollingIntervals = TEMPOARAY_polling_intervals

        while (true) {
            logger.i(
                "[Polling] Start polling Message $contentId to $operationDescription"
            )

            when (val response = messageStatusService.getStatus(contentId)) {
                is ForageApiResponse.Success -> {
                    val sqsMessage = Message.ModelMapper.from(response.data)

                    if (sqsMessage.status == "completed") {
                        if (sqsMessage.failed) {
                            return logAndReturnError(sqsMessage, operationDescription)
                        }
                        break
                    }

                    if (sqsMessage.failed) {
                        return logAndReturnError(sqsMessage, operationDescription)
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

    private fun logAndReturnError(sqsMessage: Message, operationDescription: String): ForageApiResponse.Failure {
        val sqsError = sqsMessage.errors[0]
        val isWarningLevelError = intArrayOf(400, 429)
        val message = "[Polling] Received response ${sqsError.statusCode} for $operationDescription with message: ${sqsError.message}"
        if (isWarningLevelError.contains(sqsError.statusCode)) {
            logger.w(message)
        } else {
            logger.e(message)
        }
        return sqsError.toForageError()
    }

    companion object {
        private const val MAX_ATTEMPTS = 10
    }
}
