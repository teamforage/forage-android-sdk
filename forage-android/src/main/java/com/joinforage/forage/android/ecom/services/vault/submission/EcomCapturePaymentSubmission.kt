package com.joinforage.forage.android.ecom.services.vault.submission

import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.payment.IPaymentService
import com.joinforage.forage.android.core.services.forageapi.requests.ClientApiRequest
import com.joinforage.forage.android.core.services.telemetry.LogLogger
import com.joinforage.forage.android.core.services.telemetry.UserAction
import com.joinforage.forage.android.core.services.vault.RosettaPinSubmitter
import com.joinforage.forage.android.core.services.vault.VaultPaymentMethod
import com.joinforage.forage.android.core.services.vault.errors.BaseErrorStrategy
import com.joinforage.forage.android.core.services.vault.errors.PaymentErrorStrategy
import com.joinforage.forage.android.core.services.vault.errors.PaymentMethodErrorStrategy
import com.joinforage.forage.android.core.services.vault.metrics.VaultMetricsRecorder
import com.joinforage.forage.android.core.services.vault.requests.ISubmitRequestBuilder
import com.joinforage.forage.android.core.services.vault.requests.RosettaCapturePaymentRequest
import com.joinforage.forage.android.core.services.vault.submission.PinSubmission
import com.joinforage.forage.android.ecom.services.forageapi.paymentmethod.IPaymentMethodService
import com.joinforage.forage.android.ecom.services.vault.EcomBaseBodyBuilder

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

internal class EcomCapturePaymentSubmission(
    private val paymentRef: String,
    private val vaultSubmitter: RosettaPinSubmitter,
    private val paymentMethodService: IPaymentMethodService,
    private val paymentService: IPaymentService,
    private val forageConfig: ForageConfig,
    private val logLogger: LogLogger
) : ISubmitRequestBuilder {

    override suspend fun buildRequest(
        idempotencyKey: String,
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
            idempotencyKey = idempotencyKey,
            paymentMethod = vaultPm,
            paymentRef = paymentRef,
            rawPin = vaultSubmitter.plainTextPin
        )
    }

    suspend fun submit(): ForageApiResponse<String> = PinSubmission(
        vaultSubmitter = vaultSubmitter,
        errorStrategy = PaymentErrorStrategy(
            logLogger,
            PaymentMethodErrorStrategy(
                logLogger,
                BaseErrorStrategy(logLogger)
            )
        ),
        requestBuilder = this,
        metricsRecorder = VaultMetricsRecorder(logLogger),
        userAction = UserAction.CAPTURE,
        logLogger = logLogger
    ).submit()
}
