package com.joinforage.forage.android.pos.services.vault.errors

import com.joinforage.forage.android.core.services.forageapi.network.EncryptionKeyGenerationError
import com.joinforage.forage.android.core.services.forageapi.network.FailedToReadKsnFileError
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.network.UnknownErrorApiResponse
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.EbtCard
import com.joinforage.forage.android.core.services.telemetry.LogLogger
import com.joinforage.forage.android.core.services.vault.errors.IErrorStrategy
import com.joinforage.forage.android.pos.services.encryption.dukpt.DukptCounter
import com.joinforage.forage.android.pos.services.encryption.storage.KsnFileManager

internal class PosErrorStrategy(
    private val logLogger: LogLogger,
    private val baseErrorStrategy: IErrorStrategy
) : IErrorStrategy {
    override suspend fun handleError(error: Throwable, cleanup: () -> Unit): ForageApiResponse<String> {
        return when (error) {
            is EbtCard.MissingFullPanException -> {
                cleanup()
                logLogger.e("[END] Submission failed.\n\nPaymentMethod ${error.paymentMethodRef} missing full PAN")
                UnknownErrorApiResponse
            }
            is KsnFileManager.CannotReadKsnFileException -> {
                cleanup()
                logLogger.e("[END] Submission failed.\n\nProblem reading KSN file", error)
                FailedToReadKsnFileError
            }
            is DukptCounter.InfiniteLoopException -> {
                cleanup()
                logLogger.e("[END] Submission failed.\n\nDUKPT keys out of sync causing infinite loop")
                EncryptionKeyGenerationError
            }
            else -> baseErrorStrategy.handleError(error, cleanup)
        }
    }
}
