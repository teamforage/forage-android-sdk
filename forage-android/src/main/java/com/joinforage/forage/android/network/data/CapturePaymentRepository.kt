package com.joinforage.forage.android.network.data

import com.joinforage.forage.android.core.telemetry.Log
import com.joinforage.forage.android.core.telemetry.UserAction
import com.joinforage.forage.android.model.EncryptionKeys
import com.joinforage.forage.android.model.Payment
import com.joinforage.forage.android.model.PaymentMethod
import com.joinforage.forage.android.network.EncryptionKeyService
import com.joinforage.forage.android.network.PaymentMethodService
import com.joinforage.forage.android.network.PaymentService
import com.joinforage.forage.android.network.PollingService
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.Message
import com.joinforage.forage.android.vault.AbstractVaultSubmitter
import com.joinforage.forage.android.vault.VaultSubmitter
import com.joinforage.forage.android.vault.VaultSubmitterParams

internal class CapturePaymentRepository(
    private val vaultSubmitter: VaultSubmitter,
    private val encryptionKeyService: EncryptionKeyService,
    private val pollingService: PollingService,
    private val paymentService: PaymentService,
    private val paymentMethodService: PaymentMethodService,
    private val logger: Log
) {
    suspend fun capturePayment(
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

        val vaultResponse = when (
            val response = vaultSubmitter.submit(
                params = VaultSubmitterParams(
                    encryptionKeys = encryptionKeys,
                    idempotencyKey = paymentRef,
                    merchantId = merchantId,
                    path = AbstractVaultSubmitter.capturePaymentPath(paymentRef),
                    paymentMethod = paymentMethod,
                    userAction = UserAction.CAPTURE
                )
            )
        ) {
            is ForageApiResponse.Success -> Message.ModelMapper.from(response.data)
            else -> return response
        }

        val pollingResponse = pollingService.execute(
            contentId = vaultResponse.contentId,
            operationDescription = "payment capture of Payment $payment"
        )
        if (pollingResponse is ForageApiResponse.Failure) {
            return pollingResponse
        }

        return when (val paymentResponse = paymentService.getPayment(paymentRef)) {
            is ForageApiResponse.Success -> {
                logger.i("[HTTP] Received updated Payment $paymentRef for Payment $paymentRef")
                return paymentResponse
            }
            else -> paymentResponse
        }
    }
}
