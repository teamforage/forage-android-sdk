package com.joinforage.forage.android.core.services.forageapi.paymentmethod

import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.engine.IHttpEngine
import com.joinforage.forage.android.core.services.forageapi.requests.GetPaymentMethodRequest
import com.joinforage.forage.android.ecom.services.forageapi.requests.PostEcomPaymentMethodRequest

internal class PaymentMethodResponse(val json: String) {
    val parsed = PaymentMethod(json)
}

internal interface IPaymentMethodService {
    suspend fun fetchPaymentMethod(paymentMethodRef: String): PaymentMethodResponse
    suspend fun createPaymentMethod(rawPan: String, customerId: String?, reusable: Boolean): PaymentMethodResponse
}

internal open class PaymentMethodService(
    protected val forageConfig: ForageConfig,
    protected val traceId: String,
    protected val engine: IHttpEngine
) : IPaymentMethodService {

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

    class FailedToCreatePaymentMethodException(e: Exception) :
        Exception("Failed to create payment method", e)

    override suspend fun createPaymentMethod(
        rawPan: String,
        customerId: String?,
        reusable: Boolean
    ): PaymentMethodResponse = try {
        engine.sendRequest(
            PostEcomPaymentMethodRequest(
                forageConfig,
                traceId,
                rawPan,
                customerId,
                reusable
            )
        ).let { PaymentMethodResponse(it) }
    } catch (e: Exception) {
        throw FailedToCreatePaymentMethodException(e)
    }
}
