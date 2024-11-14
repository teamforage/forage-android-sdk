package com.joinforage.forage.android.core.services.vault

import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.payment.Payment
import com.joinforage.forage.android.core.services.forageapi.payment.PaymentService
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethod
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethodService
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.services.telemetry.UserAction

internal class CapturePaymentRepository(
    private val vaultSubmitter: VaultSubmitter,
    private val paymentService: PaymentService,
    private val paymentMethodService: PaymentMethodService,
    private val logger: Log
) {
    suspend fun capturePayment(
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
            params = VaultSubmitterParams(
                idempotencyKey = paymentRef,
                merchantId = merchantId,
                path = AbstractVaultSubmitter.capturePaymentPath(paymentRef),
                paymentMethod = paymentMethod,
                userAction = UserAction.CAPTURE,
                sessionToken = sessionToken
            )
        )
    }
}
