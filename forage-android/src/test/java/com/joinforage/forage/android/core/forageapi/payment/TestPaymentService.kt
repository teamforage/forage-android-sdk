package com.joinforage.forage.android.core.forageapi.payment

import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.engine.ForageErrorResponseException
import com.joinforage.forage.android.core.services.forageapi.engine.IHttpEngine
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.payment.Payment
import com.joinforage.forage.android.core.services.forageapi.payment.PaymentService
import com.joinforage.forage.android.ecom.forageapi.refund.Refund
import com.joinforage.forage.android.ecom.forageapi.refund.RefundPaymentRequest

internal class TestPaymentService(
    forageConfig: ForageConfig,
    traceId: String,
    engine: IHttpEngine
) : PaymentService(
    forageConfig,
    traceId,
    engine
) {
    suspend fun createPayment(
        paymentMethodRef: String,
        amount: String = "1.00",
        fundingType: String = "ebt_snap",
        description: String = "test payment",
        metadata: Map<String, String> = emptyMap()
    ): Payment =
        engine.sendRequest(
            CreatePaymentRequest(
                amount = amount,
                fundingType = fundingType,
                description = description,
                metadata = metadata,
                paymentMethodRef = paymentMethodRef,
                forageConfig = forageConfig,
                traceId = traceId
            )
        ).let { Payment(it) }

    suspend fun captureDeferredPayment(paymentRef: String, accessToken: String): ForageApiResponse<String> =
        try {
            ForageApiResponse.Success(
                engine.sendRequest(
                    CaptureDeferredPaymentRequest(
                        paymentRef = paymentRef,
                        forageConfig = ForageConfig(forageConfig.merchantId, accessToken),
                        traceId = traceId
                    )
                )
            )
        } catch (e: ForageErrorResponseException) {
            ForageApiResponse.Failure(e.forageError)
        }

    suspend fun refundPayment(
        paymentRef: String,
        amount: String = "1.00",
        reason: String = "test payment",
        metadata: Map<String, String> = emptyMap()
    ): Refund =
        engine.sendRequest(
            RefundPaymentRequest(
                paymentRef = paymentRef,
                amount = amount,
                reason = reason,
                metadata = metadata,
                forageConfig = forageConfig,
                traceId = traceId
            )
        ).let { Refund(it) }

}
