package com.joinforage.forage.android.pos.services.vault

import com.joinforage.forage.android.core.services.forageapi.encryptkey.EncryptionKeyService
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.payment.PaymentService
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethodService
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.services.vault.DeferPaymentCaptureRepository
import com.joinforage.forage.android.core.services.vault.VaultSubmitter

internal class PosDeferPaymentCaptureRepository(
    vaultSubmitter: VaultSubmitter,
    encryptionKeyService: EncryptionKeyService,
    paymentMethodService: PaymentMethodService,
    paymentService: PaymentService,
    private val logger: Log
) : DeferPaymentCaptureRepository(
    vaultSubmitter,
    encryptionKeyService,
    paymentMethodService,
    paymentService
) {

    suspend fun deferPosPaymentCapture(
        merchantId: String,
        paymentRef: String,
        sessionToken: String,
        posTerminalId: String
    ): ForageApiResponse<String> {
        val response = deferPaymentCapture(merchantId, paymentRef, sessionToken)
        return if (response is ForageApiResponse.Failure) {
            logger.e(
                "[POS] deferPaymentCapture failed for Payment $paymentRef on Terminal $posTerminalId: ${response.errors[0]}"
            )
            response
        } else {
            logger.i("[POS] Successfully deferred payment capture for Payment $paymentRef")
            ForageApiResponse.Success("")
        }
    }
}
