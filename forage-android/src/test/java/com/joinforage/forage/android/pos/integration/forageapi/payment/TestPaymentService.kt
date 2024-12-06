package com.joinforage.forage.android.pos.integration.forageapi.payment

import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.ForageErrorResponseException
import com.joinforage.forage.android.core.services.forageapi.IHttpEngine
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.payment.PaymentService
import com.joinforage.forage.android.core.services.forageapi.payment.Payment

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
        posTerminalId: String,
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
                posTerminalId = posTerminalId,
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

    suspend fun captureDeferredRefund(
        paymentRef: String,
        accessToken: String,
        posTerminalId: String,
        amount: Float = 1.00f,
        reason: String = "test payment",
        metadata: Map<String, String> = emptyMap()
    ): ForageApiResponse<String> =
        try {
            ForageApiResponse.Success(
                engine.sendRequest(
                    CaptureDeferredRefundRequest(
                        paymentRef = paymentRef,
                        forageConfig = ForageConfig(forageConfig.merchantId, accessToken),
                        traceId = traceId,
                        posTerminalId = posTerminalId,
                        amount = amount,
                        reason = reason,
                        metadata = metadata
                    )
                )
            )
        } catch (e: ForageErrorResponseException) {
            ForageApiResponse.Failure(e.forageError)
        } catch (e: Exception) {
            println(e)
            ForageApiResponse.Failure(1, "")
        }
}
