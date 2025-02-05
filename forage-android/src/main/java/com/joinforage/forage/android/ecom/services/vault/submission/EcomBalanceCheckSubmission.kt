package com.joinforage.forage.android.ecom.services.vault.submission

import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.IPaymentMethodService
import com.joinforage.forage.android.core.services.forageapi.requests.ClientApiRequest
import com.joinforage.forage.android.core.services.telemetry.LogLogger
import com.joinforage.forage.android.core.services.telemetry.UserAction
import com.joinforage.forage.android.core.services.vault.IPmRefProvider
import com.joinforage.forage.android.core.services.vault.RosettaPinSubmitter
import com.joinforage.forage.android.core.services.vault.VaultPaymentMethod
import com.joinforage.forage.android.core.services.vault.errors.BaseErrorStrategy
import com.joinforage.forage.android.core.services.vault.metrics.VaultMetricsRecorder
import com.joinforage.forage.android.core.services.vault.requests.RosettaBalanceInquiryRequest
import com.joinforage.forage.android.core.services.vault.submission.BalanceCheckSubmission
import com.joinforage.forage.android.core.services.vault.submission.ISubmitDelegate
import com.joinforage.forage.android.core.services.vault.submission.PinSubmission
import com.joinforage.forage.android.ecom.services.vault.EcomBaseBodyBuilder
import com.joinforage.forage.android.ecom.services.vault.IEcomBuildRequestDelegate

private class EcomRosettaBalanceInquiryRequest(
    forageConfig: ForageConfig,
    traceId: String,
    idempotencyKey: String,
    paymentMethod: VaultPaymentMethod,
    rawPin: String,
) : RosettaBalanceInquiryRequest(
    forageConfig = forageConfig,
    traceId = traceId,
    idempotencyKey = idempotencyKey,
    paymentMethod = paymentMethod,
    body = EcomBaseBodyBuilder(rawPin).build()
)

internal class EcomBalanceCheckSubmission(
    private val paymentMethodRef: String,
    private val vaultSubmitter: RosettaPinSubmitter,
    private val paymentMethodService: IPaymentMethodService,
    private val forageConfig: ForageConfig,
    private val rawPin: String,
    private val logLogger: LogLogger
) : ISubmitDelegate, IEcomBuildRequestDelegate {

    override suspend fun buildRequest(
        paymentMethod: VaultPaymentMethod,
        idempotencyKey: String,
        traceId: String,
    ): ClientApiRequest = EcomRosettaBalanceInquiryRequest(
        forageConfig = forageConfig,
        traceId = traceId,
        idempotencyKey = idempotencyKey,
        paymentMethod = paymentMethod,
        rawPin = rawPin
    )

    private val balanceRequestBuilder = EcomSubmitRequestBuilder(this)

    override suspend fun submit(paymentMethodRefProvider: IPmRefProvider): ForageApiResponse<String> {
        return PinSubmission(
            vaultSubmitter = vaultSubmitter,
            errorStrategy = BaseErrorStrategy(logLogger), // nothing special for Ecom
            requestBuilder = balanceRequestBuilder,
            metricsRecorder = VaultMetricsRecorder(logLogger),
            paymentMethodService = paymentMethodService,
            userAction = UserAction.BALANCE,
            logLogger = logLogger
        ).submit(paymentMethodRefProvider)
    }

    suspend fun submit(): ForageApiResponse<String> = BalanceCheckSubmission(
        paymentMethodRef = paymentMethodRef,
        delegate = this
    ).submit()
}
