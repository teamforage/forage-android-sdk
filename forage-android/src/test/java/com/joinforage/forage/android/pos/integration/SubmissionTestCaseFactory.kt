package com.joinforage.forage.android.core.services.forageapi.payment

import com.joinforage.forage.android.core.logger.InMemoryLogger
import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.engine.IHttpEngine
import com.joinforage.forage.android.core.services.forageapi.requests.ClientApiRequest
import com.joinforage.forage.android.core.services.forageapi.requests.Headers
import com.joinforage.forage.android.core.services.forageapi.requests.makeApiUrl
import com.joinforage.forage.android.core.services.telemetry.LogAttributes
import com.joinforage.forage.android.core.services.telemetry.UserAction
import com.joinforage.forage.android.core.services.vault.ISecurePinCollector
import com.joinforage.forage.android.core.services.vault.RosettaPinSubmitter
import com.joinforage.forage.android.core.services.vault.errors.BaseErrorStrategy
import com.joinforage.forage.android.core.services.vault.metrics.VaultMetricsRecorder
import com.joinforage.forage.android.core.services.vault.requests.ISubmitRequestBuilder
import com.joinforage.forage.android.core.services.vault.submission.PinSubmission
import com.joinforage.forage.android.pos.TestStringResponseHttpEngine
import com.joinforage.forage.android.pos.services.emvchip.CardholderInteraction
import com.joinforage.forage.android.pos.services.emvchip.TerminalCapabilities
import com.joinforage.forage.android.pos.services.encryption.storage.InMemoryKeyRegisters
import com.joinforage.forage.android.pos.services.encryption.storage.KsnFileManager
import com.joinforage.forage.android.pos.services.vault.submission.PosBalanceCheckSubmission
import com.joinforage.forage.android.pos.services.vault.submission.PosDeferCapturePaymentSubmission
import com.joinforage.forage.android.pos.services.vault.submission.PosDeferRefundPaymentSubmission
import org.json.JSONObject

internal class TestableSecurePinCollector(private val isComplete: Boolean = true) :
    ISecurePinCollector {
    var wasCleared = false
    override fun clearText() {
        wasCleared = true
    }

    override fun isComplete(): Boolean {
        return isComplete
    }
}

internal data class SubmissionTestCase<T>(
    val submission: T,
    val logger: InMemoryLogger,
    val collector: TestableSecurePinCollector
)

internal class SubmissionTestCaseFactory(
    private val validPIN: String,
    private val forageConfig: ForageConfig,
    private val ksnFileManager: KsnFileManager,
    private val keyRegisters: InMemoryKeyRegisters,
    private val paymentRef: String,
    private val posTerminalId: String,
    private val interaction: CardholderInteraction,
    private val traceId: String,
    private val vaultHttpEngine: IHttpEngine = TestStringResponseHttpEngine("yay, success!")
) {

    fun newBalanceCheckSubmission(
        pin: String = validPIN,
        ksnFileManager: KsnFileManager = this.ksnFileManager,
        keyRegisters: InMemoryKeyRegisters = this.keyRegisters,
        vaultHttpEngine: IHttpEngine = this.vaultHttpEngine,
        logger: InMemoryLogger = InMemoryLogger(LogAttributes(forageConfig, traceId, posTerminalId))
    ): SubmissionTestCase<PosBalanceCheckSubmission> {
        val collector = TestableSecurePinCollector()
        val submission = PosBalanceCheckSubmission(
            vaultSubmitter = RosettaPinSubmitter(pin, collector, vaultHttpEngine),
            logLogger = logger,
            ksnFileManager = ksnFileManager,
            keystoreRegisters = keyRegisters,
            forageConfig = forageConfig,
            posTerminalId = posTerminalId,
            interaction = interaction,
            capabilities = TerminalCapabilities.TapAndInsert
        )
        return SubmissionTestCase(submission, logger, collector)
    }

    fun newDeferCapturePaymentSubmission(
        pin: String = validPIN,
        ksnFileManager: KsnFileManager = this.ksnFileManager,
        paymentRef: String = this.paymentRef,
        keyRegisters: InMemoryKeyRegisters = this.keyRegisters,
        vaultHttpEngine: IHttpEngine = this.vaultHttpEngine,
        logger: InMemoryLogger = InMemoryLogger(LogAttributes(forageConfig, traceId, posTerminalId))
    ): SubmissionTestCase<PosDeferCapturePaymentSubmission> {
        val collector = TestableSecurePinCollector()
        val submission = PosDeferCapturePaymentSubmission(
            vaultSubmitter = RosettaPinSubmitter(pin, collector, vaultHttpEngine),
            logLogger = logger,
            ksnFileManager = ksnFileManager,
            keystoreRegisters = keyRegisters,
            paymentRef = paymentRef,
            forageConfig = forageConfig,
            posTerminalId = posTerminalId,
            interaction = interaction,
            capabilities = TerminalCapabilities.TapAndInsert
        )
        return SubmissionTestCase(submission, logger, collector)
    }

    fun newDeferredRefundSubmission(
        pin: String = validPIN,
        ksnFileManager: KsnFileManager = this.ksnFileManager,
        paymentRef: String = this.paymentRef,
        keyRegisters: InMemoryKeyRegisters = this.keyRegisters,
        vaultHttpEngine: IHttpEngine = this.vaultHttpEngine,
        logger: InMemoryLogger = InMemoryLogger(LogAttributes(forageConfig, traceId, posTerminalId))
    ): SubmissionTestCase<PosDeferRefundPaymentSubmission> {
        val collector = TestableSecurePinCollector()
        val submission = PosDeferRefundPaymentSubmission(
            vaultSubmitter = RosettaPinSubmitter(pin, collector, vaultHttpEngine),
            logLogger = logger,
            ksnFileManager = ksnFileManager,
            keystoreRegisters = keyRegisters,
            paymentRef = paymentRef,
            forageConfig = forageConfig,
            posTerminalId = posTerminalId,
            interaction = interaction,
            capabilities = TerminalCapabilities.TapAndInsert
        )
        return SubmissionTestCase(submission, logger, collector)
    }

    fun newPinSubmissionAttempt(
        pin: String = validPIN,
        vaultHttpEngine: IHttpEngine = this.vaultHttpEngine,
        logger: InMemoryLogger = InMemoryLogger(LogAttributes(forageConfig, traceId, posTerminalId)),
        isComplete: Boolean = true
    ): SubmissionTestCase<PinSubmission> {
        val collector = TestableSecurePinCollector(isComplete = isComplete)
        val submission = PinSubmission(
            vaultSubmitter = RosettaPinSubmitter(pin, collector, vaultHttpEngine),
            errorStrategy = BaseErrorStrategy(logger),
            requestBuilder = object : ISubmitRequestBuilder {
                override suspend fun buildRequest(
                    idempotencyKey: String,
                    traceId: String,
                    vaultSubmitter: RosettaPinSubmitter
                ) = object : ClientApiRequest.PostRequest(
                    url = makeApiUrl(forageConfig, "api/payments/"),
                    forageConfig = forageConfig,
                    traceId = traceId,
                    apiVersion = Headers.ApiVersion.V_DEFAULT,
                    headers = Headers(idempotencyKey = idempotencyKey),
                    body = JSONObject()
                ) {}
            },
            metricsRecorder = VaultMetricsRecorder(logger),
            userAction = UserAction.BALANCE,
            logLogger = logger,
            traceId = traceId
        )
        return SubmissionTestCase(submission, logger, collector)
    }
}
