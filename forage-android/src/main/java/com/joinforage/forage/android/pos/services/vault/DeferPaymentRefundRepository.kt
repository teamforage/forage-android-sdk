package com.joinforage.forage.android.pos.services.vault

import com.joinforage.forage.android.core.services.forageapi.encryptkey.EncryptionKeyService
import com.joinforage.forage.android.core.services.forageapi.encryptkey.EncryptionKeys
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.payment.Payment
import com.joinforage.forage.android.core.services.forageapi.payment.PaymentService
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethod
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethodService
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.services.telemetry.UserAction
import com.joinforage.forage.android.core.services.vault.AbstractVaultSubmitter
import com.joinforage.forage.android.core.services.vault.VaultSubmitter
import com.joinforage.forage.android.core.services.vault.VaultSubmitterParams
import java.util.UUID

internal class DeferPaymentRefundRepository(
    private val vaultSubmitter: VaultSubmitter,
    private val encryptionKeyService: EncryptionKeyService,
    private val paymentMethodService: PaymentMethodService,
    private val paymentService: PaymentService,
    private val logger: Log,
) {
    /**
     * @return if successful, the response.data field is an empty string
     */
    suspend fun deferPaymentRefund(
        merchantId: String,
        paymentRef: String,
        sessionToken: String,
        posTerminalId: String,
    ): ForageApiResponse<String> {
        val encryptionKeys = when (val response = encryptionKeyService.getEncryptionKey()) {
            is ForageApiResponse.Success -> EncryptionKeys.ModelMapper.from(response.data)
            else -> return response
        }
        val payment = when (val response = paymentService.getPayment(paymentRef)) {
            is ForageApiResponse.Success -> Payment(response.data)
            else -> return response
        }
        val paymentMethod = when (val response = paymentMethodService.getPaymentMethod(payment.paymentMethodRef)) {
            is ForageApiResponse.Success -> PaymentMethod(response.data)
            else -> return response
        }

        val response = vaultSubmitter.submit(
            VaultSubmitterParams(
                encryptionKeys = encryptionKeys,
                idempotencyKey = UUID.randomUUID().toString(),
                merchantId = merchantId,
                path = AbstractVaultSubmitter.deferPaymentRefundPath(paymentRef),
                paymentMethod = paymentMethod,
                userAction = UserAction.DEFER_REFUND,
                sessionToken = sessionToken
            )
        )

        if (response is ForageApiResponse.Failure) {
            logger.e(
                "[POS] deferPaymentRefund failed for Payment $paymentRef on Terminal $posTerminalId: ${response.errors[0]}"
            )
        }
        return response
    }
}
