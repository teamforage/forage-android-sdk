package com.joinforage.forage.android.core.services.forageapi.paymentmethod

import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.engine.IHttpEngine
import com.joinforage.forage.android.core.services.forageapi.requests.GetPaymentMethodRequest

interface IPaymentMethodService {
    suspend fun fetchPaymentMethod(paymentMethodRef: String): PaymentMethod
}

internal open class PaymentMethodService(
    protected val forageConfig: ForageConfig,
    protected val traceId: String,
    protected val engine: IHttpEngine
) : IPaymentMethodService {

    class FailedToFetchPaymentMethodException(val paymentMethodRef: String, e: Exception) :
        Exception("Failed to fetch payment method $paymentMethodRef", e)

    override suspend fun fetchPaymentMethod(paymentMethodRef: String): PaymentMethod = try {
        engine.sendRequest(
            GetPaymentMethodRequest(
                paymentMethodRef,
                forageConfig,
                traceId
            )
        ).let { PaymentMethod(it) }
    } catch (e: Exception) {
        throw FailedToFetchPaymentMethodException(paymentMethodRef, e)
    }
}
