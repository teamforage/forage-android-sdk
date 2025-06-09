package com.joinforage.forage.android.core.services.forageapi.paymentmethod

import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.engine.IHttpEngine
import com.joinforage.forage.android.core.services.forageapi.requests.GetPaymentMethodRequest

internal interface IFetchPaymentMethodService {
    suspend fun fetchPaymentMethod(paymentMethodRef: String): PaymentMethodResponse
}

internal open class FetchPaymentMethodService(
    protected val forageConfig: ForageConfig,
    protected val traceId: String,
    protected val engine: IHttpEngine
) : IFetchPaymentMethodService {

    class FailedToFetchPaymentMethodException(val paymentMethodRef: String, e: Exception) :
        Exception("Failed to fetch payment method $paymentMethodRef", e)

    override suspend fun fetchPaymentMethod(paymentMethodRef: String): PaymentMethodResponse = try {
        engine.sendRequest(
            GetPaymentMethodRequest(
                paymentMethodRef,
                forageConfig,
                traceId
            )
        ).let { PaymentMethodResponse(it) }
    } catch (e: Exception) {
        throw FailedToFetchPaymentMethodException(paymentMethodRef, e)
    }
}
