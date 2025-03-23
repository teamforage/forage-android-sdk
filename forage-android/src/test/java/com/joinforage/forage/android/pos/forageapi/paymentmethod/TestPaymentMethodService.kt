package com.joinforage.forage.android.pos.forageapi.paymentmethod

import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.engine.IHttpEngine
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.FetchPaymentMethodService
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethod

internal class TestPaymentMethodService(
    forageConfig: ForageConfig,
    traceId: String,
    engine: IHttpEngine
) : FetchPaymentMethodService(
    forageConfig,
    traceId,
    engine
) {

    suspend fun createManualEntryPaymentMethod(pan: String): PaymentMethod =
        engine.sendRequest(
            CreateManualEntryPaymentMethodRequest(
                pan,
                forageConfig,
                traceId
            )
        ).let { PaymentMethod(it) }

    suspend fun createMagSwipePaymentMethod(pan: String): PaymentMethod =
        engine.sendRequest(
            CreateMagSwipePaymentMethodRequest(
                pan,
                forageConfig,
                traceId
            )
        ).let { PaymentMethod(it) }
}
