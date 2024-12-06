package com.joinforage.forage.android.pos.integration.forageapi.paymentmethod

import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.IHttpEngine
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethod
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethodService

internal class TestPaymentMethodService(
    forageConfig: ForageConfig,
    traceId: String,
    engine: IHttpEngine
) : PaymentMethodService(
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
