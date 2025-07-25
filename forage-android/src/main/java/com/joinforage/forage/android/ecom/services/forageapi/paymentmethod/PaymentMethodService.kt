package com.joinforage.forage.android.ecom.services.forageapi.paymentmethod

import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.engine.IHttpEngine
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.FetchPaymentMethodService
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.IFetchPaymentMethodService
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethodResponse
import com.joinforage.forage.android.ecom.services.forageapi.requests.PostEcomCreditPaymentMethodRequest
import com.joinforage.forage.android.ecom.services.forageapi.requests.PostEcomPaymentMethodRequest
import com.joinforage.forage.android.ecom.services.vault.CreditCardParams

internal interface ICreatePaymentMethodService {
    suspend fun createPaymentMethod(rawPan: String, customerId: String?, reusable: Boolean): PaymentMethodResponse
    suspend fun createCreditPaymentMethod(creditCardParams: CreditCardParams): PaymentMethodResponse
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

    override suspend fun createPaymentMethod(
        rawPan: String,
        customerId: String?,
        reusable: Boolean
    ): PaymentMethodResponse = engine.sendRequest(
        PostEcomPaymentMethodRequest(
            forageConfig,
            traceId,
            rawPan,
            customerId,
            reusable
        )
    ).let { PaymentMethodResponse(it) }

    override suspend fun createCreditPaymentMethod(creditCardParams: CreditCardParams): PaymentMethodResponse =
        engine.sendRequest(
            PostEcomCreditPaymentMethodRequest(
                forageConfig,
                traceId,
                creditCardParams
            )
        ).let { PaymentMethodResponse(it) }
}
