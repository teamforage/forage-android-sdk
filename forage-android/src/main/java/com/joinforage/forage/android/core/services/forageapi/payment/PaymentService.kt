package com.joinforage.forage.android.core.services.forageapi.payment

import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.GetPaymentRequest
import com.joinforage.forage.android.core.services.forageapi.IHttpEngine

interface IPaymentService {
    suspend fun fetchPayment(paymentRef: String): Payment
}

internal open class PaymentService(
    protected val forageConfig: ForageConfig,
    protected val traceId: String,
    protected val engine: IHttpEngine
) : IPaymentService {
    class FailedToFetchPaymentException(val paymentRef: String, e: Exception) :
        Exception("Failed to fetch payment $paymentRef", e)

    override suspend fun fetchPayment(paymentRef: String): Payment = try {
        engine.sendRequest(
            GetPaymentRequest(
                paymentRef,
                forageConfig,
                traceId
            )
        ).let { Payment(it) }
    } catch (e: Exception) {
        throw FailedToFetchPaymentException(paymentRef, e)
    }
}
