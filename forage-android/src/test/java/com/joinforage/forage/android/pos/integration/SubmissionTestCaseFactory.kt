package com.joinforage.forage.android.core.services.forageapi.payment

import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.UserAction
import com.joinforage.forage.android.core.services.forageapi.ClientApiRequest
import com.joinforage.forage.android.core.services.forageapi.Headers
import com.joinforage.forage.android.core.services.forageapi.IHttpEngine
import com.joinforage.forage.android.pos.TestStringResponseHttpEngine
import com.joinforage.forage.android.core.services.forageapi.makeApiUrl
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.EbtCard
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.IPaymentMethodService
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethod
import com.joinforage.forage.android.core.services.telemetry.LogAttributes
import com.joinforage.forage.android.core.services.vault.errors.BaseErrorStrategy
import com.joinforage.forage.android.core.services.vault.submission.PinSubmission
import com.joinforage.forage.android.core.services.vault.metrics.VaultMetricsRecorder
import com.joinforage.forage.android.core.services.vault.ISecurePinCollector
import com.joinforage.forage.android.core.services.vault.requests.ISubmitRequestBuilder
import com.joinforage.forage.android.core.services.vault.RosettaPinSubmitter
import com.joinforage.forage.android.pos.integration.logger.InMemoryLogger
import com.joinforage.forage.android.pos.services.CardholderInteraction
import com.joinforage.forage.android.pos.services.TerminalCapabilities
import com.joinforage.forage.android.pos.services.encryption.storage.InMemoryKeyRegisters
import com.joinforage.forage.android.pos.services.encryption.storage.KsnFileManager
import com.joinforage.forage.android.pos.services.vault.submission.PosBalanceCheckSubmission
import com.joinforage.forage.android.pos.services.vault.submission.PosCapturePaymentSubmission
import com.joinforage.forage.android.pos.services.vault.submission.PosDeferCapturePaymentSubmission
import com.joinforage.forage.android.pos.services.vault.submission.PosDeferRefundPaymentSubmission
import com.joinforage.forage.android.pos.services.vault.submission.PosRefundPaymentSubmission
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
    private val paymentMethodRef: String,
    private val paymentRef: String,
    private val posTerminalId: String,
    private val interaction: CardholderInteraction,
    private val traceId: String,
    private val paymentMethodService: IPaymentMethodService = createMockPaymentMethodService(paymentMethodRef),
    private val paymentService: IPaymentService = createMockPaymentService(paymentMethodRef),
    private val vaultHttpEngine: IHttpEngine = TestStringResponseHttpEngine("yay, success!")
) {

    companion object {

        fun createMockPaymentMethodService(pmRef: String): IPaymentMethodService =
            object : IPaymentMethodService {
                override suspend fun fetchPaymentMethod(paymentMethodRef: String): PaymentMethod =
                    PaymentMethod(
                        ref = pmRef,
                        type = "ebt",
                        customerId = "dummy_customer_id",
                        balance = null,
                        card = EbtCard(
                            last4 = "7777",
                            fingerprint = "dummy_fingerprint",
                            token = "dummy_token",
                            number = "6777777777777777",
                            usState = null
                        ),
                        reusable = true
                    )
            }

        fun createMockPaymentService(pmRef: String): IPaymentService =
            object : IPaymentService {
                override suspend fun fetchPayment(paymentRef: String): Payment =
                    Payment(
                        amount = "1.00",
                        created = "2024-01-01T00:00:00Z",
                        deliveryAddress = null,
                        description = "test payment",
                        fundingType = "ebt_snap",
                        isDelivery = false,
                        merchant = "dummy_merchant",
                        metadata = null,
                        paymentMethodRef = pmRef,
                        receipt = null,
                        ref = "dummy_payment_ref",
                        refunds = emptyList(),
                        status = "succeeded",
                        successDate = "2024-01-01T00:00:00Z",
                        updated = "2024-01-01T00:00:00Z"
                    )
            }
    }

    fun newBalanceCheckSubmission(
        pin: String = validPIN,
        ksnFileManager: KsnFileManager = this.ksnFileManager,
        keyRegisters: InMemoryKeyRegisters = this.keyRegisters,
        paymentMethodService: IPaymentMethodService = this.paymentMethodService,
        vaultHttpEngine: IHttpEngine = this.vaultHttpEngine,
        logger: InMemoryLogger = InMemoryLogger(LogAttributes(forageConfig, traceId, posTerminalId))
    ): SubmissionTestCase<PosBalanceCheckSubmission> {
        val collector = TestableSecurePinCollector()
        val submission = PosBalanceCheckSubmission(
            vaultSubmitter = RosettaPinSubmitter(pin, collector, vaultHttpEngine),
            logLogger = logger,
            ksnFileManager = ksnFileManager,
            keystoreRegisters = keyRegisters,
            paymentMethodService = paymentMethodService,
            forageConfig = forageConfig,
            paymentMethodRef = paymentMethodRef,
            posTerminalId = posTerminalId,
            interaction = interaction,
            capabilities = TerminalCapabilities.TapAndInsert
        )
        return SubmissionTestCase(submission, logger, collector)
    }

    fun newCapturePaymentSubmission(
        pin: String = validPIN,
        ksnFileManager: KsnFileManager = this.ksnFileManager,
        paymentRef: String = this.paymentRef,
        keyRegisters: InMemoryKeyRegisters = this.keyRegisters,
        paymentMethodService: IPaymentMethodService = this.paymentMethodService,
        paymentService: IPaymentService = this.paymentService,
        vaultHttpEngine: IHttpEngine = this.vaultHttpEngine,
        logger: InMemoryLogger = InMemoryLogger(LogAttributes(forageConfig, traceId, posTerminalId))
    ): SubmissionTestCase<PosCapturePaymentSubmission> {
        val collector = TestableSecurePinCollector()
        val submission = PosCapturePaymentSubmission(
            vaultSubmitter = RosettaPinSubmitter(pin, collector, vaultHttpEngine),
            logLogger = logger,
            ksnFileManager = ksnFileManager,
            keystoreRegisters = keyRegisters,
            paymentMethodService = paymentMethodService,
            paymentService = paymentService,
            paymentRef = paymentRef,
            forageConfig = forageConfig,
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
        paymentMethodService: IPaymentMethodService = this.paymentMethodService,
        paymentService: IPaymentService = this.paymentService,
        vaultHttpEngine: IHttpEngine = this.vaultHttpEngine,
        logger: InMemoryLogger = InMemoryLogger(LogAttributes(forageConfig, traceId, posTerminalId))
    ): SubmissionTestCase<PosDeferCapturePaymentSubmission> {
        val collector = TestableSecurePinCollector()
        val submission = PosDeferCapturePaymentSubmission(
            vaultSubmitter = RosettaPinSubmitter(pin, collector, vaultHttpEngine),
            logLogger = logger,
            ksnFileManager = ksnFileManager,
            keystoreRegisters = keyRegisters,
            paymentMethodService = paymentMethodService,
            paymentService = paymentService,
            paymentRef = paymentRef,
            forageConfig = forageConfig,
            interaction = interaction,
            capabilities = TerminalCapabilities.TapAndInsert
        )
        return SubmissionTestCase(submission, logger, collector)
    }

    fun newRefundPaymentSubmission(
        pin: String = validPIN,
        ksnFileManager: KsnFileManager = this.ksnFileManager,
        paymentRef: String = this.paymentRef,
        keyRegisters: InMemoryKeyRegisters = this.keyRegisters,
        paymentMethodService: IPaymentMethodService = this.paymentMethodService,
        paymentService: IPaymentService = this.paymentService,
        vaultHttpEngine: IHttpEngine = this.vaultHttpEngine,
        logger: InMemoryLogger = InMemoryLogger(LogAttributes(forageConfig, traceId, posTerminalId)),
        amount: Float,
        reason: String = "integration test refund",
        metadata: Map<String, String> = emptyMap()
    ): SubmissionTestCase<PosRefundPaymentSubmission> {
        val collector = TestableSecurePinCollector()
        val submission = PosRefundPaymentSubmission(
            vaultSubmitter = RosettaPinSubmitter(pin, collector, vaultHttpEngine),
            logLogger = logger,
            ksnFileManager = ksnFileManager,
            keystoreRegisters = keyRegisters,
            paymentMethodService = paymentMethodService,
            paymentService = paymentService,
            paymentRef = paymentRef,
            forageConfig = forageConfig,
            posTerminalId = posTerminalId,
            interaction = interaction,
            capabilities = TerminalCapabilities.TapAndInsert,
            amount = amount,
            reason = reason,
            metadata = metadata
        )
        return SubmissionTestCase(submission, logger, collector)
    }

    fun newDeferredRefundSubmission(
        pin: String = validPIN,
        ksnFileManager: KsnFileManager = this.ksnFileManager,
        paymentRef: String = this.paymentRef,
        keyRegisters: InMemoryKeyRegisters = this.keyRegisters,
        paymentMethodService: IPaymentMethodService = this.paymentMethodService,
        paymentService: IPaymentService = this.paymentService,
        vaultHttpEngine: IHttpEngine = this.vaultHttpEngine,
        logger: InMemoryLogger = InMemoryLogger(LogAttributes(forageConfig, traceId, posTerminalId))
    ): SubmissionTestCase<PosDeferRefundPaymentSubmission> {
        val collector = TestableSecurePinCollector()
        val submission = PosDeferRefundPaymentSubmission(
            vaultSubmitter = RosettaPinSubmitter(pin, collector, vaultHttpEngine),
            logLogger = logger,
            ksnFileManager = ksnFileManager,
            keystoreRegisters = keyRegisters,
            paymentMethodService = paymentMethodService,
            paymentService = paymentService,
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
        paymentMethodService: IPaymentMethodService = this.paymentMethodService,
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
                    paymentMethod: PaymentMethod,
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
            paymentMethodService = paymentMethodService,
            userAction = UserAction.BALANCE,
            logLogger = logger,
            traceId = traceId
        )
        return SubmissionTestCase(submission, logger, collector)
    }
}
