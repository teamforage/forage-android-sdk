package com.joinforage.forage.android.core.services.forageapi.payment

import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.engine.IHttpEngine
import com.joinforage.forage.android.core.services.forageapi.requests.GetPaymentRequest
import com.joinforage.forage.android.core.services.telemetry.LogLogger
import javax.inject.Inject
import javax.inject.Named

interface IPaymentService {
    suspend fun fetchPayment(paymentRef: String): Payment
}

internal open class PaymentService @Inject constructor(
    protected val forageConfig: ForageConfig,
    protected val logger: LogLogger,
    @Named("api") protected val engine: IHttpEngine
) : IPaymentService {
    class FailedToFetchPaymentException(val paymentRef: String, e: Exception) :
        Exception("Failed to fetch payment $paymentRef", e)

    override suspend fun fetchPayment(paymentRef: String): Payment = try {
        engine.sendRequest(
            GetPaymentRequest(
                paymentRef,
                forageConfig,
                logger.traceId
            )
        ).let { Payment(it) }
    } catch (e: Exception) {
        throw FailedToFetchPaymentException(paymentRef, e)
    }
}
