package com.joinforage.forage.android.core.services.vault.submission

import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.payment.IPaymentService
import com.joinforage.forage.android.core.services.vault.IPmRefProvider

internal class PaymentSubmission(
    private val paymentService: IPaymentService,
    private val paymentRef: String,
    private val delegate: ISubmitDelegate
) {
    private val paymentMethodRefProvider = object : IPmRefProvider {
        override suspend fun getPaymentMethodRef(): String = paymentService.fetchPayment(paymentRef).paymentMethodRef
    }

    suspend fun submit(): ForageApiResponse<String> = delegate.submit(
        paymentMethodRefProvider
    )
}
