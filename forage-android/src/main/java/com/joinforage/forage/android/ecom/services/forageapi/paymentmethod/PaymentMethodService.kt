package com.joinforage.forage.android.ecom.services.forageapi.paymentmethod

import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.engine.IHttpEngine
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.FetchPaymentMethodService
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.IFetchPaymentMethodService
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethodResponse
import com.joinforage.forage.android.ecom.services.forageapi.requests.PostEcomPaymentMethodRequest

internal interface ICreatePaymentMethodService {
    suspend fun createPaymentMethod(rawPan: String, customerId: String?, reusable: Boolean): PaymentMethodResponse
}

internal interface IPaymentMethodService : IFetchPaymentMethodService, ICreatePaymentMethodService

internal class PaymentMethodService(
    forageConfig: ForageConfig,
    traceId: String,
    engine: IHttpEngine
) : FetchPaymentMethodService(
    forageConfig,
    traceId,
    engine
),
    IPaymentMethodService {

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
