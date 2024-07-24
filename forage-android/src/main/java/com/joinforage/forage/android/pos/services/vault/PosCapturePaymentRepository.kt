package com.joinforage.forage.android.pos.services.vault

import com.joinforage.forage.android.core.services.forageapi.encryptkey.EncryptionKeyService
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.payment.PaymentService
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethodService
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.services.vault.CapturePaymentRepository
import com.joinforage.forage.android.core.services.vault.VaultSubmitter

internal class PosCapturePaymentRepository(
    vaultSubmitter: VaultSubmitter,
    encryptionKeyService: EncryptionKeyService,
    paymentService: PaymentService,
    paymentMethodService: PaymentMethodService,
    logger: Log
) : CapturePaymentRepository(
    vaultSubmitter,
    encryptionKeyService,
    paymentService,
    paymentMethodService,
    logger
) {
    suspend fun capturePosPayment(
        merchantId: String,
        paymentRef: String,
        sessionToken: String,
        posTerminalId: String
    ): ForageApiResponse<String> {
        val response = capturePayment(merchantId, paymentRef, sessionToken)
        if (response is ForageApiResponse.Failure) {
            logger.e(
                "[POS] capturePayment failed for payment $paymentRef on Terminal $posTerminalId: ${response.errors[0]}",
                attributes = mapOf(
                    "payment_ref" to paymentRef,
                    "pos_terminal_id" to posTerminalId
                )
            )
        }
        return response
    }
}
