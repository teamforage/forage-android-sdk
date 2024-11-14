package com.joinforage.forage.android.core.services.vault

import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.payment.Payment
import com.joinforage.forage.android.core.services.forageapi.payment.PaymentService
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethod
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethodService
import com.joinforage.forage.android.core.services.telemetry.UserAction
import java.util.UUID

internal class DeferPaymentCaptureRepository(
    private val vaultSubmitter: VaultSubmitter,
    private val paymentMethodService: PaymentMethodService,
    private val paymentService: PaymentService
) {
    /**
     * @return if successful, the response.data field is an empty string
     */
    suspend fun deferPaymentCapture(
        merchantId: String,
        paymentRef: String,
        sessionToken: String
    ): ForageApiResponse<String> {
        val paymentMethodRef = when (val response = paymentService.getPayment(paymentRef)) {
            is ForageApiResponse.Success -> Payment.getPaymentMethodRef(response.data)
            else -> return response
        }
        val paymentMethod = when (val response = paymentMethodService.getPaymentMethod(paymentMethodRef)) {
            is ForageApiResponse.Success -> PaymentMethod(response.data)
            else -> return response
        }

        return vaultSubmitter.submit(
            VaultSubmitterParams(
                idempotencyKey = UUID.randomUUID().toString(),
                merchantId = merchantId,
                path = AbstractVaultSubmitter.deferPaymentCapturePath(paymentRef),
                paymentMethod = paymentMethod,
                userAction = UserAction.DEFER_CAPTURE,
                sessionToken = sessionToken
            )
        )
    }
}
