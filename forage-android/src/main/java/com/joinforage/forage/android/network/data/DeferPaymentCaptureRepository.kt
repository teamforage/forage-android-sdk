package com.joinforage.forage.android.network.data

import com.joinforage.forage.android.core.telemetry.UserAction
import com.joinforage.forage.android.model.EncryptionKeys
import com.joinforage.forage.android.model.Payment
import com.joinforage.forage.android.model.PaymentMethod
import com.joinforage.forage.android.network.EncryptionKeyService
import com.joinforage.forage.android.network.PaymentMethodService
import com.joinforage.forage.android.network.PaymentService
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.vault.AbstractVaultSubmitter
import com.joinforage.forage.android.vault.VaultSubmitter
import com.joinforage.forage.android.vault.VaultSubmitterParams
import java.util.UUID

internal class DeferPaymentCaptureRepository(
    private val vaultSubmitter: VaultSubmitter,
    private val encryptionKeyService: EncryptionKeyService,
    private val paymentMethodService: PaymentMethodService,
    private val paymentService: PaymentService
) {
    /**
     * @return if successful, the response.data field is an empty string
     */
    suspend fun deferPaymentCapture(
        idempotencyKey: String = UUID.randomUUID().toString(),
        merchantId: String,
        paymentRef: String
    ): ForageApiResponse<String> {
        val encryptionKeys = when (val response = encryptionKeyService.getEncryptionKey()) {
            is ForageApiResponse.Success -> EncryptionKeys.ModelMapper.from(response.data)
            else -> return response
        }
        val payment = when (val response = paymentService.getPayment(paymentRef)) {
            is ForageApiResponse.Success -> Payment.ModelMapper.from(response.data)
            else -> return response
        }
        val paymentMethod = when (val response = paymentMethodService.getPaymentMethod(payment.paymentMethod)) {
            is ForageApiResponse.Success -> PaymentMethod.ModelMapper.from(response.data)
            else -> return response
        }

        return vaultSubmitter.submit(
            VaultSubmitterParams(
                encryptionKeys = encryptionKeys,
                idempotencyKey = idempotencyKey,
                merchantId = merchantId,
                path = AbstractVaultSubmitter.deferPaymentCapturePath(paymentRef),
                paymentMethod = paymentMethod,
                userAction = UserAction.DEFER_CAPTURE
            )
        )
    }
}
