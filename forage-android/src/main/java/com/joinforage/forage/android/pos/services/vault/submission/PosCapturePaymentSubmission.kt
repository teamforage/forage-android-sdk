package com.joinforage.forage.android.pos.services.vault.submission

import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.requests.ClientApiRequest
import com.joinforage.forage.android.core.services.telemetry.LogLogger
import com.joinforage.forage.android.core.services.telemetry.UserAction
import com.joinforage.forage.android.core.services.vault.RosettaPinSubmitter
import com.joinforage.forage.android.core.services.vault.errors.BaseErrorStrategy
import com.joinforage.forage.android.core.services.vault.metrics.VaultMetricsRecorder
import com.joinforage.forage.android.core.services.vault.requests.RosettaCapturePaymentRequest
import com.joinforage.forage.android.core.services.vault.submission.ISubmitDelegate
import com.joinforage.forage.android.core.services.vault.submission.PinSubmission
import com.joinforage.forage.android.pos.services.emvchip.CardholderInteraction
import com.joinforage.forage.android.pos.services.emvchip.TerminalCapabilities
import com.joinforage.forage.android.pos.services.encryption.dukpt.SecureKeyStorageRegisters
import com.joinforage.forage.android.pos.services.encryption.storage.KsnFileManager
import com.joinforage.forage.android.pos.services.vault.errors.PosErrorStrategy
import com.joinforage.forage.android.pos.services.vault.requests.IPosBuildRequestDelegate
import com.joinforage.forage.android.pos.services.vault.requests.PosBaseBodyBuilder

private class PosRosettaCapturePaymentRequest(
    forageConfig: ForageConfig,
    posTerminalId: String,
    traceId: String,
    idempotencyKey: String,
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
    paymentRef = paymentRef,
    body = PosBaseBodyBuilder(
        encryptedPinBlock,
        keySerialNumber,
        txnCounter,
        interaction,
        capabilities,
        posTerminalId
    ).build()
)

internal class PosCapturePaymentSubmission(
    private val paymentRef: String,
    private val vaultSubmitter: RosettaPinSubmitter,
    ksnFileManager: KsnFileManager,
    keystoreRegisters: SecureKeyStorageRegisters,
    interaction: CardholderInteraction,
    private val capabilities: TerminalCapabilities,
    private val forageConfig: ForageConfig,
    private val posTerminalId: String,
    private val logLogger: LogLogger
) : ISubmitDelegate, IPosBuildRequestDelegate {

    override suspend fun buildRequest(
        idempotencyKey: String,
        traceId: String,
        pinTranslationParams: PinTranslationParams,
        interaction: CardholderInteraction
    ): ClientApiRequest = PosRosettaCapturePaymentRequest(
        forageConfig = forageConfig,
        posTerminalId = posTerminalId,
        traceId = traceId,
        idempotencyKey = idempotencyKey,
        encryptedPinBlock = pinTranslationParams.encryptedPinBlock,
        keySerialNumber = pinTranslationParams.keySerialNumber,
        txnCounter = pinTranslationParams.txnCounter,
        capabilities = capabilities,
        paymentRef = paymentRef,
        interaction = interaction
    )

    private val captureRequestBuilder = PosSubmitRequestBuilder(
        ksnFileManager,
        keystoreRegisters,
        interaction,
        delegate = this
    )

    override suspend fun rawSubmit(): ForageApiResponse<String> {
        return PinSubmission(
            vaultSubmitter = vaultSubmitter,
            errorStrategy = PosErrorStrategy(logLogger, BaseErrorStrategy(logLogger)),
            requestBuilder = captureRequestBuilder,
            metricsRecorder = VaultMetricsRecorder(logLogger),
            userAction = UserAction.CAPTURE,
            logLogger = logLogger
        ).submit()
    }

    suspend fun submit(): ForageApiResponse<String> = rawSubmit()
}
