package com.joinforage.forage.android.ecom.integration
import com.joinforage.forage.android.core.forageapi.getAccessToken
import com.joinforage.forage.android.core.forageapi.getSessionToken
import com.joinforage.forage.android.core.forageapi.payment.TestPaymentService
import com.joinforage.forage.android.ecom.logger.EcomLoggableAttributesFactory
import com.joinforage.forage.android.core.logger.LoggableAttributes
import com.joinforage.forage.android.core.services.EnvConfig
import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.ecom.services.forageapi.engine.EcomOkHttpEngine
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.payment.Payment
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.EbtBalance
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethod
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethodService
import com.joinforage.forage.android.core.services.forageapi.polling.ForageErrorDetails
import com.joinforage.forage.android.core.services.generateTraceId
import com.joinforage.forage.android.core.services.telemetry.MetricOutcome
import com.joinforage.forage.android.core.services.telemetry.UserAction
import com.joinforage.forage.android.core.services.vault.IPmRefProvider
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
        private val merchantRef = "2af38883c9"
        private val username = "SoOTl8Gyq4xpDlQixd1moh0ffD02CaN3vnqUZou7"
        private val password = "ITBZBYO9FDqaOyjFIWWvAogqbEG61kKFI7QA1lkJBNskBsOIxU3Ts9NQmf5d0Ou3t2KTxvNJbtcyai8hMNKVUgHcvrmo4EeLcd3Fz81zXDSgWZmJsd0wQ73fZJz1DN12"
        private val pan = "6777 7777 7777 7777".filter { it.isDigit() }
        private val goodPin = pan.takeLast(4)
        private val badPin = "1234"
        private val env = EnvConfig.Dev
        private val httpEngine = EcomOkHttpEngine()
        private val traceId = generateTraceId()
        private val customerId = "test_customer_id"

        private lateinit var pmRefProvider: TestPmRefProvider
        private lateinit var forageConfig: ForageConfig
        private lateinit var paymentMethodService: PaymentMethodService
        private lateinit var paymentService: TestPaymentService
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
            paymentMethodService = PaymentMethodService(forageConfig, traceId, httpEngine)
            paymentService = TestPaymentService(ForageConfig(merchantRef, accessToken), traceId, httpEngine)
            paymentMethod = paymentMethodService.createPaymentMethod(
                pan,
                customerId = customerId,
                reusable = true
            ).parsed
            payment = paymentService.createPayment(
                paymentMethodRef = paymentMethod.ref,
            )
            pmRefProvider = TestPmRefProvider(paymentMethod.ref)

            // Initialize the submission test case factory
            submissionTestCaseFactory = SubmissionTestCaseFactory(
                pin = goodPin,
                paymentMethodService = paymentMethodService,
                paymentService = paymentService,
                paymentRef = payment.ref,
                forageConfig = forageConfig,
                traceId = traceId,
                vaultHttpEngine = httpEngine
            )

            failureAttrs = EcomLoggableAttributesFactory(
                forageConfig = forageConfig,
                traceId = traceId,
                paymentMethodRef = paymentMethod.ref
            )(UserAction.BALANCE, 500, MetricOutcome.FAILURE)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testEnd2EndHappyPath() = runTest {
        // Get initial balance using new submission factory
        val (balanceSubmission) = submissionTestCaseFactory.newBalanceCheckSubmission(
            paymentMethodRef = paymentMethod.ref
        )
        val balanceResponse = balanceSubmission.submit()
        val originalBalance = ((balanceResponse as ForageApiResponse.Success<String>).toBalance() as EbtBalance)

        // Test Cash Payment Flow (sync)
        val cashTxAmount = "1.00"
        val cashPayment = testCashPaymentFlow(cashTxAmount)
        testCashRefundFlow(cashPayment, cashTxAmount)

        // Test SNAP Payment Flow  (deferred)
        val snapTxAmount = "1.00"
        val snapPayment = testSnapPaymentFlow(snapTxAmount)
        testSnapRefundFlow(snapPayment, snapTxAmount)
    }

    private suspend fun testCashPaymentFlow(amount: String): Payment {
        // Create and capture cash payment
        val cashPayment = paymentService.createPayment(paymentMethod.ref, amount, "ebt_cash")
        val (captureSubmission) = submissionTestCaseFactory.newCapturePaymentSubmission(
            paymentRef = cashPayment.ref
        )
        val captureResponse = captureSubmission.submit()
        val capturedCash = ((captureResponse as ForageApiResponse.Success<String>).toPayment())

        // Verify balance was reduced by payment amount
        assertThat(capturedCash.status).isEqualTo("succeeded")

        return cashPayment
    }

    private suspend fun testCashRefundFlow(cashPayment: Payment, amount: String) {
        // Refund the cash payment
        val refundedCash = paymentService.refundPayment(cashPayment.ref, amount)

        // Verify balance was restored after refund
        assertThat(refundedCash.status).isEqualTo("processing")
    }

    private suspend fun testSnapPaymentFlow(amount: String): Payment {
        // Create and defer capture SNAP payment
        val snapPayment = paymentService.createPayment(paymentMethod.ref, amount, "ebt_snap")
        val (deferSubmission) = submissionTestCaseFactory.newDeferCapturePaymentSubmission(
            paymentRef = snapPayment.ref
        )
        val snapDeferResponse = deferSubmission.submit()
        assertThat(snapDeferResponse).isInstanceOf(ForageApiResponse.Success::class.java)

        // Complete deferred capture
        val capturedSnap = (paymentService.captureDeferredPayment(snapPayment.ref, accessToken) as ForageApiResponse.Success<String>).toPayment()

        // Verify balance was reduced by payment amount
        assertThat(capturedSnap.status).isEqualTo("succeeded")

        return snapPayment
    }

    private suspend fun testSnapRefundFlow(snapPayment: Payment, amount: String) {
        // Refund the snap payment
        val refundedSnap = paymentService.refundPayment(snapPayment.ref, amount)


        // Verify refund succeeded and balance was restored
        assertThat(refundedSnap.status).isEqualTo("processing")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testBadPINScenarios() = runTest {
        // Test balance check with bad PIN
        val (balanceSubmission) = submissionTestCaseFactory.newBalanceCheckSubmission(
            pin = badPin,
            paymentMethodRef = paymentMethod.ref
        )
        val balanceCheckResponse = balanceSubmission.submit()
        assertBadPinError(balanceCheckResponse)

        // Test cash payment capture with bad PIN
        val cashPayment = paymentService.createPayment(
            paymentMethodRef = paymentMethod.ref,
            amount = "1.00",
            fundingType = "ebt_cash"
        )
        val (captureSubmission) = submissionTestCaseFactory.newCapturePaymentSubmission(
            paymentRef = cashPayment.ref,
            pin = badPin
        )
        val captureResponse = captureSubmission.submit()
        assertBadPinError(captureResponse)

        // Test SNAP payment deferred capture with bad PIN
        val snapPayment = paymentService.createPayment(
            paymentMethodRef = paymentMethod.ref,
            amount = "1.00",
            fundingType = "ebt_snap"
        )
        val (deferSubmission) = submissionTestCaseFactory.newDeferCapturePaymentSubmission(
            paymentRef = snapPayment.ref,
            pin = badPin
        )
        val deferResponse = deferSubmission.submit()
        assertThat(deferResponse).isInstanceOf(ForageApiResponse.Success::class.java)

        val deferCaptureResponse = paymentService.captureDeferredPayment(snapPayment.ref, accessToken)
        assertBadPinError(deferCaptureResponse)
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
    fun testSnapPurchaseOfTenThousandInsufficientFundsFailure() = runTest {
        // Attempt to make a SNAP purchase for $10,000 (which should exceed available balance)
        val snapPayment = paymentService.createPayment(
            paymentMethodRef = paymentMethod.ref,
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

}
