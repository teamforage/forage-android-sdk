package com.joinforage.forage.android.ecom.services.vault.submission

import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.IPaymentMethodService
import com.joinforage.forage.android.core.services.forageapi.requests.ClientApiRequest
import com.joinforage.forage.android.core.services.forageapi.requests.RosettaVaultRequest
import com.joinforage.forage.android.core.services.telemetry.LogLogger
import com.joinforage.forage.android.core.services.telemetry.UserAction
import com.joinforage.forage.android.core.services.vault.RosettaPinSubmitter
import com.joinforage.forage.android.core.services.vault.VaultPaymentMethod
import com.joinforage.forage.android.core.services.vault.errors.BaseErrorStrategy
import com.joinforage.forage.android.core.services.vault.errors.PaymentMethodErrorStrategy
import com.joinforage.forage.android.core.services.vault.metrics.VaultMetricsRecorder
import com.joinforage.forage.android.core.services.vault.requests.ISubmitRequestBuilder
import com.joinforage.forage.android.core.services.vault.submission.BalanceCheckSubmission
import com.joinforage.forage.android.core.services.vault.submission.ISubmitDelegate
import com.joinforage.forage.android.core.services.vault.submission.PinSubmission
import com.joinforage.forage.android.ecom.services.vault.EcomBaseBodyBuilder

private class EcomRosettaBalanceInquiryRequest(
    forageConfig: ForageConfig,
    traceId: String,
    idempotencyKey: String,
    paymentMethod: VaultPaymentMethod,
    rawPin: String
) : RosettaVaultRequest(
    path = "proxy/api/payment_methods/${paymentMethod.ref}/balance/",
    forageConfig = forageConfig,
    traceId = traceId,
    idempotencyKey = idempotencyKey,
    body = EcomBaseBodyBuilder(rawPin, paymentMethod.token).build()
)

internal class EcomBalanceCheckSubmission(
    private val paymentMethodRef: String,
    private val vaultSubmitter: RosettaPinSubmitter,
    private val paymentMethodService: IPaymentMethodService,
    private val forageConfig: ForageConfig,
    private val logLogger: LogLogger
) : ISubmitRequestBuilder, ISubmitDelegate {

    override suspend fun buildRequest(
        idempotencyKey: String,
        traceId: String,
        vaultSubmitter: RosettaPinSubmitter
    ): ClientApiRequest {
        logLogger.setPaymentMethodRef(paymentMethodRef)
        val paymentMethod = paymentMethodService.fetchPaymentMethod(paymentMethodRef).parsed
        val token = vaultSubmitter.getVaultToken(paymentMethod)
        val vaultPm = VaultPaymentMethod(
            ref = paymentMethod.ref,
            token = token
        )
        return EcomRosettaBalanceInquiryRequest(
            forageConfig = forageConfig,
            traceId = traceId,
            idempotencyKey = idempotencyKey,
            paymentMethod = vaultPm,
            rawPin = vaultSubmitter.plainTextPin
        )
    }

    override suspend fun rawSubmit(): ForageApiResponse<String> {
        return PinSubmission(
            vaultSubmitter = vaultSubmitter,
            errorStrategy = PaymentMethodErrorStrategy(
                logLogger,
                BaseErrorStrategy(logLogger)
            ),
            requestBuilder = this,
            metricsRecorder = VaultMetricsRecorder(logLogger),
            userAction = UserAction.BALANCE,
            logLogger = logLogger
        ).submit()
    }

    suspend fun submit(): ForageApiResponse<String> =
        BalanceCheckSubmission(this).submit()
}
