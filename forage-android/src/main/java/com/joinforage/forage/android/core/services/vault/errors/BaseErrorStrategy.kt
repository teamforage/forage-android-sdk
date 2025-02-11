package com.joinforage.forage.android.core.services.vault.errors

import com.joinforage.forage.android.core.services.forageapi.engine.HttpRequestFailedException
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.network.IncompletePinError
import com.joinforage.forage.android.core.services.forageapi.network.UnknownErrorApiResponse
import com.joinforage.forage.android.core.services.forageapi.network.UnknownTimeoutErrorResponse
import com.joinforage.forage.android.core.services.forageapi.network.error.payload.ErrorPayload
import com.joinforage.forage.android.core.services.telemetry.LogLogger
import com.joinforage.forage.android.core.services.vault.RosettaPinSubmitter
import com.joinforage.forage.android.core.services.vault.submission.PinSubmission

internal class BaseErrorStrategy(private val logLogger: LogLogger) : IErrorStrategy {
    override suspend fun handleError(error: Throwable, cleanup: () -> Unit): ForageApiResponse<String> {
        return when (error) {
            is PinSubmission.UserIncompletePinException -> {
                cleanup()
                logLogger.w("[END] Submission failed.\n\nPin is incomplete.")
                IncompletePinError
            }

            is RosettaPinSubmitter.VaultForageErrorResponseException -> {
                cleanup()
                logLogger.w("[END] Submission failed.\n\nForage Proxy response is:\n\n${error.failure.error}")
                error.failure
            }
            is ErrorPayload.UnknownForageFailureResponse -> {
                cleanup()
                logLogger.e("[END] Submission failed.\n\nMalformed Forage API response is ${error.rawResponse}")
                UnknownErrorApiResponse
            }
            is HttpRequestFailedException -> {
                cleanup()
                logLogger.e("[END] Submission failed.\n\nFailed HTTP request", error.cause)
                when (error.cause) {
                    is java.net.SocketTimeoutException -> UnknownTimeoutErrorResponse
                    else -> UnknownErrorApiResponse
                }
            }
            is RosettaPinSubmitter.MissingTokenException -> {
                cleanup()
                logLogger.e("[END] Submission failed.\n\nVault missing token for PaymentMethod ${error.paymentMethodRef}")
                UnknownErrorApiResponse
            }
            else -> {
                cleanup()
                logLogger.e("[END] Submission failed.\n\nUnknown error occurred", error)
                UnknownErrorApiResponse
            }
        }
    }
}
