package com.joinforage.forage.android.pos.integration
import com.joinforage.forage.android.core.services.EnvConfig
import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.engine.OkHttpEngine
import com.joinforage.forage.android.core.services.forageapi.network.EncryptionKeyGenerationError
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.payment.Payment
import com.joinforage.forage.android.core.services.forageapi.payment.SubmissionTestCaseFactory
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.EbtBalance
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethod
import com.joinforage.forage.android.core.services.forageapi.polling.ForageErrorDetails
import com.joinforage.forage.android.core.services.generateTraceId
import com.joinforage.forage.android.core.services.telemetry.LogAttributes
import com.joinforage.forage.android.core.services.telemetry.Loggable
import com.joinforage.forage.android.core.services.telemetry.MetricOutcome
import com.joinforage.forage.android.core.services.telemetry.UserAction
import com.joinforage.forage.android.core.services.vault.IPmRefProvider
import com.joinforage.forage.android.pos.integration.base64.JavaBase64Util
import com.joinforage.forage.android.pos.integration.forageapi.getAccessToken
import com.joinforage.forage.android.pos.integration.forageapi.getSessionToken
import com.joinforage.forage.android.pos.integration.forageapi.payment.TestPaymentService
import com.joinforage.forage.android.pos.integration.forageapi.paymentmethod.TestPaymentMethodService
import com.joinforage.forage.android.pos.integration.logger.InMemoryLogger
import com.joinforage.forage.android.pos.integration.logger.LoggableAttributes
import com.joinforage.forage.android.pos.integration.logger.LoggableAttributesFactory
import com.joinforage.forage.android.pos.services.emvchip.ManualEntryInteraction
import com.joinforage.forage.android.pos.services.encryption.certificate.RsaKeyManager
import com.joinforage.forage.android.pos.services.encryption.dukpt.DukptService
import com.joinforage.forage.android.pos.services.encryption.storage.InMemoryKeyRegisters
import com.joinforage.forage.android.pos.services.encryption.storage.StringKsnManager
import com.joinforage.forage.android.pos.services.forageapi.refund.Refund
import com.joinforage.forage.android.pos.services.init.PosTerminalInitializer
import com.joinforage.forage.android.pos.services.init.RosettaInitService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test

class MainIntegrationTest {

    private class TestPmRefProvider(private val pmRef: String) : IPmRefProvider {
        override suspend fun getPaymentMethodRef(): String = pmRef
    }

    companion object {
        private val merchantRef = "e6b746712a" // "c67e8569c1"
        private val posTerminalId = "HeadlessAndroidIntegrationTests"
        private val username = "o3vJFaHmO3eOGLxhREmwk7GHIAD4k7E9WTOwGeUP"
        private val password = "BrqSz3vDhb98nwW2wJ7OpZtx5eQYTKuJGhAD4BxSKKk0yvBNjBy6yVArn1wpFQJX618yo2oA4PUCyRWJj4SflMuhPGSGj4kaJXK158uMJvOdtT5CU4uVyeopfpx3ooDx"
        private val pan = "6777 7777 7777 7777".filter { it.isDigit() }
        private val interaction = ManualEntryInteraction(pan)
        private val badPIN = "1234"
        private val validPIN = pan.takeLast(4)
        private val env = EnvConfig.Dev
        private val httpEngine = OkHttpEngine()
        private val traceId = generateTraceId()
        private val ksnFileManager = StringKsnManager()
        private val keyRegisters = InMemoryKeyRegisters()

        private lateinit var pmRefProvider: TestPmRefProvider
        private lateinit var forageConfig: ForageConfig
        private lateinit var paymentMethodService: TestPaymentMethodService
        private lateinit var paymentService: TestPaymentService
        private lateinit var rosetta: RosettaInitService
        private lateinit var paymentMethod: PaymentMethod
        private lateinit var payment: Payment
        private lateinit var failureAttrs: LoggableAttributes
        private lateinit var accessToken: String
        private lateinit var submissionTestCaseFactory: SubmissionTestCaseFactory

        @BeforeClass
        @JvmStatic
        fun setupClass() = runBlocking {
            println("The Trace ID for this test run is: $traceId")
            accessToken = getAccessToken(username, password, env)
            val sessionToken = getSessionToken(accessToken, merchantRef)
            forageConfig = ForageConfig(merchantRef, sessionToken)
            paymentMethodService = TestPaymentMethodService(forageConfig, traceId, httpEngine)
            paymentService = TestPaymentService(forageConfig, traceId, httpEngine)
            rosetta = RosettaInitService(
                forageConfig,
                traceId,
                posTerminalId,
                httpEngine
            )
            paymentMethod = paymentMethodService.createManualEntryPaymentMethod(pan)
            payment = paymentService.createPayment(
                paymentMethodRef = paymentMethod.ref,
                posTerminalId = posTerminalId
            )
            pmRefProvider = TestPmRefProvider(paymentMethod.ref)

            // Initialize the submission test case factory
            submissionTestCaseFactory = SubmissionTestCaseFactory(
                validPIN = validPIN,
                forageConfig = forageConfig,
                ksnFileManager = ksnFileManager,
                keyRegisters = keyRegisters,
                paymentMethodRef = paymentMethod.ref,
                paymentRef = payment.ref,
                posTerminalId = posTerminalId,
                interaction = interaction,
                traceId = traceId,
                paymentMethodService = paymentMethodService,
                paymentService = paymentService,
                vaultHttpEngine = httpEngine
            )

            // only do this once...
            PosTerminalInitializer(
                ksnFileManager,
                InMemoryLogger(LogAttributes(forageConfig, traceId, posTerminalId)),
                rosetta,
                keyRegisters,
                JavaBase64Util(),
                RsaKeyManager(JavaBase64Util())
            ) { ksn -> DukptService(ksn, keyRegisters) }.safeInit()

            failureAttrs = LoggableAttributesFactory(
                forageConfig = forageConfig,
                traceId = traceId,
                posTerminalId = posTerminalId,
                paymentMethodRef = paymentMethod.ref
            )(UserAction.BALANCE, 500, MetricOutcome.FAILURE)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testEnd2EndHappyPath() = runTest {
        // Get initial balance using new submission factory
        val (balanceSubmission) = submissionTestCaseFactory.newBalanceCheckSubmission()
        val balanceResponse = balanceSubmission.submit()
        val originalBalance = ((balanceResponse as ForageApiResponse.Success<String>).toBalance() as EbtBalance)

        // Test Cash Payment Flow (sync)
        val cashTxAmount = "1.00"
        val cashPayment = testCashPaymentFlow(originalBalance, cashTxAmount)
        testCashRefundFlow(originalBalance, cashPayment, cashTxAmount)

        // Test SNAP Payment Flow  (deferred)
        val snapTxAmount = "1.00"
        val snapPayment = testSnapPaymentFlow(originalBalance, snapTxAmount)
        testSnapRefundFlow(originalBalance, snapPayment, snapTxAmount)
    }

    private suspend fun testCashPaymentFlow(originalBalance: EbtBalance, amount: String): Payment {
        // Create and capture cash payment
        val cashPayment = paymentService.createPayment(paymentMethod.ref, posTerminalId, amount, "ebt_cash")
        val (captureSubmission) = submissionTestCaseFactory.newCapturePaymentSubmission(
            paymentRef = cashPayment.ref
        )
        val captureResponse = captureSubmission.submit()
        val capturedCash = ((captureResponse as ForageApiResponse.Success<String>).toPayment())

        // Verify balance was reduced by payment amount
        assertThat(capturedCash.status).isEqualTo("succeeded")
//        assertThat((capturedCash.receipt!!.balance as EbtBalance).cash.toFloat())
//            .isEqualTo(originalBalance.cash.toFloat() - amount.toFloat())

        return cashPayment
    }

    private suspend fun testCashRefundFlow(originalBalance: EbtBalance, cashPayment: Payment, amount: String) {
        // Refund the cash payment
        val (refundSubmission) = submissionTestCaseFactory.newRefundPaymentSubmission(
            paymentRef = cashPayment.ref,
            amount = amount.toFloat()
        )
        val refundResponse = refundSubmission.submit()
        val refundedCash = Refund((refundResponse as ForageApiResponse.Success<String>).data)

        // Verify balance was restored after refund
        assertThat(refundedCash.status).isEqualTo("succeeded")
//        assertThat((refundedCash.receipt!!.balance as EbtBalance).cash.toFloat())
//            .isEqualTo(originalBalance.cash.toFloat())
    }

    private suspend fun testSnapPaymentFlow(originalBalance: EbtBalance, amount: String): Payment {
        // Create and defer capture SNAP payment
        val snapPayment = paymentService.createPayment(paymentMethod.ref, posTerminalId, amount, "ebt_snap")
        val (deferSubmission) = submissionTestCaseFactory.newDeferCapturePaymentSubmission(
            paymentRef = snapPayment.ref
        )
        val snapDeferResponse = deferSubmission.submit()
        assertThat(snapDeferResponse).isInstanceOf(ForageApiResponse.Success::class.java)

        // Complete deferred capture
        val capturedSnap = (paymentService.captureDeferredPayment(snapPayment.ref, accessToken) as ForageApiResponse.Success<String>).toPayment()

        // Verify balance was reduced by payment amount
        assertThat(capturedSnap.status).isEqualTo("succeeded")
//        assertThat((capturedSnap.receipt!!.balance as EbtBalance).snap.toFloat())
//            .isEqualTo(originalBalance.snap.toFloat() - amount.toFloat())

        return snapPayment
    }

    private suspend fun testSnapRefundFlow(originalBalance: EbtBalance, snapPayment: Payment, amount: String) {
        // Create deferred refund
        val (deferredRefundSubmission) = submissionTestCaseFactory.newDeferredRefundSubmission(
            paymentRef = snapPayment.ref
        )
        val snapDeferredRefundResponse = deferredRefundSubmission.submit()
        assertThat(snapDeferredRefundResponse).isInstanceOf(ForageApiResponse.Success::class.java)

        // Complete deferred refund
        val snapRefundResult = paymentService.captureDeferredRefund(
            snapPayment.ref,
            accessToken,
            posTerminalId,
            amount.toFloat()
        ) as ForageApiResponse.Success<String>

        val refundedSnap = Refund(snapRefundResult.data)

        // Verify refund succeeded and balance was restored
        assertThat(refundedSnap.status).isEqualTo("succeeded")
//        assertThat((refundedSnap.receipt!!.balance as EbtBalance).snap.toFloat())
//            .isEqualTo(originalBalance.snap.toFloat())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testBadPINScenarios() = runTest {
        // Test balance check with bad PIN
        val (balanceSubmission) = submissionTestCaseFactory.newBalanceCheckSubmission(pin = badPIN)
        val balanceCheckResponse = balanceSubmission.submit()
        assertBadPinError(balanceCheckResponse)

        // Test cash payment capture with bad PIN
        val cashPayment = paymentService.createPayment(
            paymentMethodRef = paymentMethod.ref,
            posTerminalId = posTerminalId,
            amount = "1.00",
            fundingType = "ebt_cash"
        )
        val (captureSubmission) = submissionTestCaseFactory.newCapturePaymentSubmission(
            paymentRef = cashPayment.ref,
            pin = badPIN
        )
        val captureResponse = captureSubmission.submit()
        assertBadPinError(captureResponse)

        // Test SNAP payment deferred capture with bad PIN
        val snapPayment = paymentService.createPayment(
            paymentMethodRef = paymentMethod.ref,
            posTerminalId = posTerminalId,
            amount = "1.00",
            fundingType = "ebt_snap"
        )
        val (deferSubmission) = submissionTestCaseFactory.newDeferCapturePaymentSubmission(
            paymentRef = snapPayment.ref,
            pin = badPIN
        )
        val deferResponse = deferSubmission.submit()
        assertThat(deferResponse).isInstanceOf(ForageApiResponse.Success::class.java)

        val deferCaptureResponse = paymentService.captureDeferredPayment(snapPayment.ref, accessToken)
        assertBadPinError(deferCaptureResponse)

        // Capture the cash payment first with valid PIN
        val (captureSuccessSubmission) = submissionTestCaseFactory.newCapturePaymentSubmission(
            paymentRef = cashPayment.ref,
            pin = validPIN
        )
        val captureSuccessResponse = captureSuccessSubmission.submit()
        assertThat(captureSuccessResponse).isInstanceOf(ForageApiResponse.Success::class.java)

        // Test refund with bad PIN on captured payment
        val (refundSubmission) = submissionTestCaseFactory.newRefundPaymentSubmission(
            paymentRef = cashPayment.ref,
            pin = badPIN,
            amount = 1.00f
        )
        val refundResponse = refundSubmission.submit()
        assertBadPinError(refundResponse)

        // Test deferred refund with bad PIN on captured payment
        val (deferredRefundSubmission) = submissionTestCaseFactory.newDeferredRefundSubmission(
            paymentRef = cashPayment.ref,
            pin = badPIN
        )
        val deferredRefundResponse = deferredRefundSubmission.submit()
        assertThat(deferredRefundResponse).isInstanceOf(ForageApiResponse.Success::class.java)

        val deferredRefundCaptureResponse = paymentService.captureDeferredRefund(
            cashPayment.ref,
            accessToken,
            posTerminalId,
            1.00f
        )
        assertBadPinError(deferredRefundCaptureResponse)

        // Finally restore the balance with a successful refund
        val (successfulRefundSubmission) = submissionTestCaseFactory.newRefundPaymentSubmission(
            paymentRef = cashPayment.ref,
            pin = validPIN,
            amount = 1.00f
        )
        val successfulRefundResponse = successfulRefundSubmission.submit()
        assertThat(successfulRefundResponse).isInstanceOf(ForageApiResponse.Success::class.java)
    }

    private fun assertBadPinError(response: ForageApiResponse<String>) {
        val actualRes = response as ForageApiResponse.Failure
        val expectedRes = ForageApiResponse.Failure(
            400,
            "ebt_error_55",
            "Invalid PIN or PIN not selected - Invalid PIN"
        )
        assertThat(actualRes).isEqualTo(expectedRes)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testDukptInfiniteLoopException() = runTest {
        // Create a clone of the KSN file manager before any operations
        val staleKsnManager = ksnFileManager.clone()

        // Make an initial balance check to advance the key registers
        val (initialSubmission) = submissionTestCaseFactory.newBalanceCheckSubmission()
        val initialResponse = initialSubmission.submit()
        assertThat(initialResponse).isInstanceOf(ForageApiResponse.Success::class.java)

        // Now create a new submission with the stale KSN manager
        // This should cause an infinite loop when trying to find the next key
        val (
            submission,
            logger,
            collector
        ) = submissionTestCaseFactory.newBalanceCheckSubmission(
            ksnFileManager = staleKsnManager
        )

        val response = submission.submit(pmRefProvider)
        assertThat(response).isEqualTo(EncryptionKeyGenerationError)

        // Verify the collector was cleared
        assertThat(collector.wasCleared).isTrue

        // Verify the logs show the DUKPT infinite loop error
        val expectedLogs = listOf(
            Loggable.Info("[balance]", "[START] Submit Attempt", failureAttrs.logAttrs.noPM),
            Loggable.Error(
                "[balance]",
                "[END] Submission failed.\n\nDUKPT keys out of sync causing infinite loop",
                null,
                failureAttrs.logAttrs.all
            )
        )
        assertThat(logger.logs).isEqualTo(expectedLogs)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testSnapPurchaseOfTenThousandInsufficientFundsFailure() = runTest {
        // Attempt to make a SNAP purchase for $10,000 (which should exceed available balance)
        val snapPayment = paymentService.createPayment(
            paymentMethodRef = paymentMethod.ref,
            posTerminalId = posTerminalId,
            amount = "10000.00",
            fundingType = "ebt_snap"
        )

        val (deferSubmission) = submissionTestCaseFactory.newDeferCapturePaymentSubmission(
            paymentRef = snapPayment.ref
        )
        val snapDeferResponse = deferSubmission.submit()
        assertThat(snapDeferResponse).isInstanceOf(ForageApiResponse.Success::class.java)

        // Attempt to capture the deferred payment
        val captureResponse = paymentService.captureDeferredPayment(snapPayment.ref, accessToken)
        captureResponse as ForageApiResponse.Failure
        val error = captureResponse.error
        val details = error.details as ForageErrorDetails.EbtError51Details
        assertThat(error.code).isEqualTo("ebt_error_51")
        assertThat(details.snapBalance).isInstanceOf(String::class.java)
        assertThat(details.cashBalance).isInstanceOf(String::class.java)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testSnapRefundOfTenThousandIsGreaterThanPurchase() = runTest {
        // First make a small SNAP purchase
        val snapTxAmount = "1.00"
        val snapPayment = paymentService.createPayment(
            paymentMethodRef = paymentMethod.ref,
            posTerminalId = posTerminalId,
            amount = snapTxAmount,
            fundingType = "ebt_snap"
        )

        // Defer capture the payment
        val (deferSubmission) = submissionTestCaseFactory.newDeferCapturePaymentSubmission(
            paymentRef = snapPayment.ref
        )
        val snapDeferResponse = deferSubmission.submit()
        assertThat(snapDeferResponse).isInstanceOf(ForageApiResponse.Success::class.java)

        // Complete the deferred capture
        val captureResponse = paymentService.captureDeferredPayment(snapPayment.ref, accessToken)
        assertThat(captureResponse).isInstanceOf(ForageApiResponse.Success::class.java)

        // Now attempt to refund for much more than the original purchase
        val largeRefundAmount = 10000.00f
        val (refundSubmission) = submissionTestCaseFactory.newRefundPaymentSubmission(
            paymentRef = snapPayment.ref,
            amount = largeRefundAmount
        )
        val refundResponse = refundSubmission.submit()

        // Verify the refund fails with appropriate error
        val actualRes = refundResponse as ForageApiResponse.Failure
        val expectedRes = ForageApiResponse.Failure(
            400,
            "refund_exceeds_order_total",
            "Requested refund would push the total amount " +
                "refunded above the original Payment amount. Total amount " +
                "paid is 1.00. Existing total of refunds is 0"
        )
        assertThat(actualRes).isEqualTo(expectedRes)

        // Clean up by refunding the original amount
        val (cleanupRefundSubmission) = submissionTestCaseFactory.newRefundPaymentSubmission(
            paymentRef = snapPayment.ref,
            amount = snapTxAmount.toFloat()
        )
        val cleanupResponse = cleanupRefundSubmission.submit()
        assertThat(cleanupResponse).isInstanceOf(ForageApiResponse.Success::class.java)
    }
}

// TODO: cover some passing Track2 data cases. Right now everything is manual entry!
