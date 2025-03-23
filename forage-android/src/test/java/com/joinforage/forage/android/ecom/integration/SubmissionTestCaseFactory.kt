package com.joinforage.forage.android.ecom.integration

import com.joinforage.forage.android.core.logger.InMemoryLogger
import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.engine.IHttpEngine
import com.joinforage.forage.android.core.services.forageapi.payment.PaymentService
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
import com.joinforage.forage.android.ecom.TestStringResponseHttpEngine
import com.joinforage.forage.android.ecom.services.forageapi.paymentmethod.IPaymentMethodService
import com.joinforage.forage.android.ecom.services.vault.TokenizeCardService
import com.joinforage.forage.android.ecom.services.vault.submission.EcomBalanceCheckSubmission
import com.joinforage.forage.android.ecom.services.vault.submission.EcomCapturePaymentSubmission
import com.joinforage.forage.android.ecom.services.vault.submission.EcomDeferCapturePaymentSubmission
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
    private val pin: String,
    private val forageConfig: ForageConfig,
    private val paymentMethodService: IPaymentMethodService,
    private val paymentService: PaymentService,
    private val paymentRef: String,
    private val traceId: String,
    private val vaultHttpEngine: IHttpEngine = TestStringResponseHttpEngine("yay, success!")
) {

    fun newEbtCardTokenizer(
        logger: InMemoryLogger = InMemoryLogger(LogAttributes(forageConfig, traceId))
    ) = TokenizeCardService(
        logger,
        forageConfig,
        this.paymentMethodService
    )

    fun newBalanceCheckSubmission(
        pin: String = this.pin,
        paymentMethodRef: String,
        vaultHttpEngine: IHttpEngine = this.vaultHttpEngine,
        logger: InMemoryLogger = InMemoryLogger(LogAttributes(forageConfig, traceId))
    ): SubmissionTestCase<EcomBalanceCheckSubmission> {
        val collector = TestableSecurePinCollector()
        val submission = EcomBalanceCheckSubmission(
            vaultSubmitter = RosettaPinSubmitter(pin, collector, vaultHttpEngine),
            paymentMethodRef = paymentMethodRef,
            paymentMethodService = paymentMethodService,
            logLogger = logger,
            forageConfig = forageConfig
        )
        return SubmissionTestCase(submission, logger, collector)
    }

    fun newCapturePaymentSubmission(
        pin: String = this.pin,
        paymentRef: String = this.paymentRef,
        vaultHttpEngine: IHttpEngine = this.vaultHttpEngine,
        logger: InMemoryLogger = InMemoryLogger(LogAttributes(forageConfig, traceId))
    ): SubmissionTestCase<EcomCapturePaymentSubmission> {
        val collector = TestableSecurePinCollector()
        val submission = EcomCapturePaymentSubmission(
            vaultSubmitter = RosettaPinSubmitter(pin, collector, vaultHttpEngine),
            logLogger = logger,
            paymentMethodService = paymentMethodService,
            paymentService = paymentService,
            paymentRef = paymentRef,
            forageConfig = forageConfig
        )
        return SubmissionTestCase(submission, logger, collector)
    }

    fun newDeferCapturePaymentSubmission(
        pin: String = this.pin,
        paymentRef: String = this.paymentRef,
        vaultHttpEngine: IHttpEngine = this.vaultHttpEngine,
        logger: InMemoryLogger = InMemoryLogger(LogAttributes(forageConfig, traceId))
    ): SubmissionTestCase<EcomDeferCapturePaymentSubmission> {
        val collector = TestableSecurePinCollector()
        val submission = EcomDeferCapturePaymentSubmission(
            vaultSubmitter = RosettaPinSubmitter(pin, collector, vaultHttpEngine),
            logLogger = logger,
            paymentRef = paymentRef,
            forageConfig = forageConfig,
            paymentMethodService = paymentMethodService,
            paymentService = paymentService
        )
        return SubmissionTestCase(submission, logger, collector)
    }

    fun newPinSubmissionAttempt(
        pin: String = this.pin,
        vaultHttpEngine: IHttpEngine = this.vaultHttpEngine,
        logger: InMemoryLogger = InMemoryLogger(LogAttributes(forageConfig, traceId)),
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
