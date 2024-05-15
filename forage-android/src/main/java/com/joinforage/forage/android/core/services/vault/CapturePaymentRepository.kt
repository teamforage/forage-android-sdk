package com.joinforage.forage.android.core.services.vault

import com.joinforage.forage.android.core.services.forageapi.encryptkey.EncryptionKeyService
import com.joinforage.forage.android.core.services.forageapi.encryptkey.EncryptionKeys
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.payment.Payment
import com.joinforage.forage.android.core.services.forageapi.payment.PaymentService
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethod
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethodService
import com.joinforage.forage.android.core.services.forageapi.polling.Message
import com.joinforage.forage.android.core.services.forageapi.polling.PollingService
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.services.telemetry.UserAction

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
        paymentRef: String,
        sessionToken: String
    ): ForageApiResponse<String> {
        val encryptionKeys = when (val response = encryptionKeyService.getEncryptionKey()) {
            is ForageApiResponse.Success -> EncryptionKeys.ModelMapper.from(response.data)
            else -> return response
        }
        val paymentMethodRef = when (val response = paymentService.getPayment(paymentRef)) {
            is ForageApiResponse.Success -> Payment.getPaymentMethodRef(response.data)
            else -> return response
        }
        val paymentMethod = when (val response = paymentMethodService.getPaymentMethod(paymentMethodRef)) {
            is ForageApiResponse.Success -> PaymentMethod(response.data)
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
                    userAction = UserAction.CAPTURE,
                    sessionToken = sessionToken
                )
            )
        ) {
            is ForageApiResponse.Success -> Message.ModelMapper.from(response.data)
            else -> return response
        }

        val pollingResponse = pollingService.execute(
            contentId = vaultResponse.contentId,
            operationDescription = "payment capture of Payment $paymentRef"
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
