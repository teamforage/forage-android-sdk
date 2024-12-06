package com.joinforage.forage.android.pos.services.vault.submission

import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.UserAction
import com.joinforage.forage.android.core.services.forageapi.ClientApiRequest
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.payment.IPaymentService
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.IPaymentMethodService
import com.joinforage.forage.android.core.services.telemetry.LogLogger
import com.joinforage.forage.android.core.services.vault.errors.BaseErrorStrategy
import com.joinforage.forage.android.core.services.vault.errors.PaymentErrorStrategy
import com.joinforage.forage.android.core.services.vault.submission.PaymentSubmission
import com.joinforage.forage.android.core.services.vault.submission.PinSubmission
import com.joinforage.forage.android.core.services.vault.metrics.VaultMetricsRecorder
import com.joinforage.forage.android.core.services.vault.IPmRefProvider
import com.joinforage.forage.android.core.services.vault.submission.ISubmitDelegate
import com.joinforage.forage.android.core.services.vault.RosettaPinSubmitter
import com.joinforage.forage.android.core.services.vault.VaultPaymentMethod
import com.joinforage.forage.android.pos.services.CardholderInteraction
import com.joinforage.forage.android.pos.services.TerminalCapabilities
import com.joinforage.forage.android.pos.services.encryption.dukpt.SecureKeyStorageRegisters
import com.joinforage.forage.android.pos.services.encryption.storage.KsnFileManager
import com.joinforage.forage.android.pos.services.vault.requests.IPosBuildRequestDelegate
import com.joinforage.forage.android.pos.services.vault.errors.PosErrorStrategy
import com.joinforage.forage.android.pos.services.vault.requests.PosBaseBodyBuilder
import com.joinforage.forage.android.core.services.vault.requests.RosettaCapturePaymentRequest

private class PosRosettaCapturePaymentRequest(
    forageConfig: ForageConfig,
    traceId: String,
    idempotencyKey: String,
    paymentMethod: VaultPaymentMethod,
    encryptedPinBlock: String,
    keySerialNumber: String,
    interaction: CardholderInteraction,
    txnCounter: String,
    capabilities: TerminalCapabilities,
    paymentRef: String
) : RosettaCapturePaymentRequest(
    forageConfig = forageConfig,
    traceId = traceId,
    idempotencyKey = idempotencyKey,
    paymentMethod = paymentMethod,
    paymentRef = paymentRef,
    body = PosBaseBodyBuilder(
        encryptedPinBlock,
        keySerialNumber,
        txnCounter,
        interaction,
        capabilities
    ).build()
)

internal class PosCapturePaymentSubmission(
    private val paymentRef: String,
    private val vaultSubmitter: RosettaPinSubmitter,
    private val paymentMethodService: IPaymentMethodService,
    private val paymentService: IPaymentService,
    ksnFileManager: KsnFileManager,
    keystoreRegisters: SecureKeyStorageRegisters,
    interaction: CardholderInteraction,
    private val capabilities: TerminalCapabilities,
    private val forageConfig: ForageConfig,
    private val logLogger: LogLogger
) : ISubmitDelegate, IPosBuildRequestDelegate {

    override suspend fun buildRequest(
        paymentMethod: VaultPaymentMethod,
        idempotencyKey: String,
        traceId: String,
        pinTranslationParams: PinTranslationParams,
        interaction: CardholderInteraction
    ): ClientApiRequest = PosRosettaCapturePaymentRequest(
        forageConfig = forageConfig,
        traceId = traceId,
        idempotencyKey = idempotencyKey,
        encryptedPinBlock = pinTranslationParams.encryptedPinBlock,
        keySerialNumber = pinTranslationParams.keySerialNumber,
        txnCounter = pinTranslationParams.txnCounter,
        capabilities = capabilities,
        paymentRef = paymentRef,
        paymentMethod = paymentMethod,
        interaction = interaction
    )

    private val captureRequestBuilder = PosSubmitRequestBuilder(
        ksnFileManager,
        keystoreRegisters,
        interaction,
        delegate = this
    )

    override suspend fun submit(paymentMethodRefProvider: IPmRefProvider): ForageApiResponse<String> {
        return PinSubmission(
            vaultSubmitter = vaultSubmitter,
            errorStrategy = PaymentErrorStrategy(logLogger, PosErrorStrategy(logLogger, BaseErrorStrategy(logLogger))),
            requestBuilder = captureRequestBuilder,
            metricsRecorder = VaultMetricsRecorder(logLogger),
            paymentMethodService = paymentMethodService,
            userAction = UserAction.CAPTURE,
            logLogger = logLogger
        ).submit(paymentMethodRefProvider)
    }

    suspend fun submit(): ForageApiResponse<String> = PaymentSubmission(
        paymentService = paymentService,
        paymentRef = paymentRef,
        delegate = this
    ).submit()
}
