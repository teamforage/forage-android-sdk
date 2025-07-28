package com.joinforage.forage.android.ecom.services.vault.submission

import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.payment.IPaymentService
import com.joinforage.forage.android.core.services.forageapi.requests.ClientApiRequest
import com.joinforage.forage.android.core.services.telemetry.LogLogger
import com.joinforage.forage.android.core.services.telemetry.UserAction
import com.joinforage.forage.android.core.services.vault.ISecurePinCollector
import com.joinforage.forage.android.core.services.vault.RosettaPinSubmitter
import com.joinforage.forage.android.core.services.vault.VaultPaymentMethod
import com.joinforage.forage.android.core.services.vault.errors.BaseErrorStrategy
import com.joinforage.forage.android.core.services.vault.errors.PaymentErrorStrategy
import com.joinforage.forage.android.core.services.vault.errors.PaymentMethodErrorStrategy
import com.joinforage.forage.android.core.services.vault.requests.ISubmitRequestBuilder
import com.joinforage.forage.android.core.services.vault.requests.RosettaCapturePaymentRequest
import com.joinforage.forage.android.ecom.services.PinSubmissionFactory
import com.joinforage.forage.android.ecom.services.forageapi.paymentmethod.IPaymentMethodService
import com.joinforage.forage.android.ecom.services.vault.EcomBaseBodyBuilder
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

private class EcomRosettaCapturePaymentRequest(
    forageConfig: ForageConfig,
    traceId: String,
    idempotencyKey: String,
    paymentMethod: VaultPaymentMethod,
    paymentRef: String,
    rawPin: String
) : RosettaCapturePaymentRequest(
    forageConfig = forageConfig,
    traceId = traceId,
    idempotencyKey = idempotencyKey,
    paymentRef = paymentRef,
    body = EcomBaseBodyBuilder(rawPin, paymentMethod.token).build()
)

internal class EcomCapturePaymentSubmission @AssistedInject constructor(
    @Assisted private val paymentRef: String,
    private val pinCollector: ISecurePinCollector?,
    private val paymentMethodService: IPaymentMethodService,
    private val paymentService: IPaymentService,
    private val forageConfig: ForageConfig,
    private val logLogger: LogLogger,
    private val pinSubmissionFactory: PinSubmissionFactory
) : ISubmitRequestBuilder {
    override suspend fun buildRequest(
        traceId: String,
        vaultSubmitter: RosettaPinSubmitter
    ): ClientApiRequest {
        val paymentMethodRef = paymentService.fetchPayment(paymentRef).paymentMethodRef
        logLogger.setPaymentMethodRef(paymentMethodRef)
        val paymentMethod = paymentMethodService.fetchPaymentMethod(paymentMethodRef).parsed
        val token = vaultSubmitter.getVaultToken(paymentMethod)
        val vaultPm = VaultPaymentMethod(ref = paymentMethod.ref, token = token)
        return EcomRosettaCapturePaymentRequest(
            forageConfig = forageConfig,
            traceId = traceId,
            idempotencyKey = paymentRef,
            paymentMethod = vaultPm,
            paymentRef = paymentRef,
            rawPin = pinCollector!!.getPin()
        )
    }

    suspend fun submit(): ForageApiResponse<String> = pinSubmissionFactory.build(
        errorStrategy = PaymentErrorStrategy(
            logLogger,
            PaymentMethodErrorStrategy(
                logLogger,
                BaseErrorStrategy(logLogger)
            )
        ),
        requestBuilder = this,
        userAction = UserAction.CAPTURE
    ).submit()
}
