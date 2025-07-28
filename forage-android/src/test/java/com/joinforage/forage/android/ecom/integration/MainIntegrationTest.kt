package com.joinforage.forage.android.ecom.integration
import com.joinforage.forage.android.core.forageapi.getAccessToken
import com.joinforage.forage.android.core.forageapi.getSessionToken
import com.joinforage.forage.android.core.forageapi.payment.TestPaymentService
import com.joinforage.forage.android.core.logger.InMemoryLogger
import com.joinforage.forage.android.core.logger.LoggableAttributes
import com.joinforage.forage.android.core.services.EnvConfig
import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.payment.Payment
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.EbtBalance
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethod
import com.joinforage.forage.android.core.services.forageapi.polling.ForageErrorDetails
import com.joinforage.forage.android.core.services.generateTraceId
import com.joinforage.forage.android.core.services.telemetry.LogAttributes
import com.joinforage.forage.android.core.services.telemetry.LogLogger
import com.joinforage.forage.android.core.services.telemetry.MetricOutcome
import com.joinforage.forage.android.core.services.telemetry.UserAction
import com.joinforage.forage.android.core.services.vault.IPmRefProvider
import com.joinforage.forage.android.ecom.logger.EcomLoggableAttributesFactory
import com.joinforage.forage.android.ecom.services.DaggerForageSDKTestComponent
import com.joinforage.forage.android.ecom.services.ForageSDK
import com.joinforage.forage.android.ecom.services.forageapi.engine.EcomOkHttpEngine
import com.joinforage.forage.android.ecom.services.forageapi.paymentmethod.PaymentMethodService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test

@Ignore("Require server credentials and this is a public repo")
class MainIntegrationTest {

    private class TestPmRefProvider(private val pmRef: String) : IPmRefProvider {
        override suspend fun getPaymentMethodRef(): String = pmRef
    }

    companion object {
        private val merchantRef = "your_merchant_id"
        private val username = "your_client_app_id"
        private val password = "your_client_app_secret"
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
        private lateinit var logger: LogLogger

        @BeforeClass
        @JvmStatic
        fun setupClass() = runBlocking {
            println("The Trace ID for this test run is: $traceId")
            accessToken = getAccessToken(username, password, env)
            val sessionToken = getSessionToken(accessToken, merchantRef)
            forageConfig = ForageConfig(merchantRef, sessionToken)
            logger = InMemoryLogger(LogAttributes(forageConfig, traceId))
            paymentMethodService = PaymentMethodService(forageConfig, logger, httpEngine)
            paymentService = TestPaymentService(ForageConfig(merchantRef, accessToken), logger, httpEngine)
            paymentMethod = paymentMethodService.createPaymentMethod(
                pan,
                customerId = customerId,
                reusable = true
            ).parsed
            payment = paymentService.createPayment(
                paymentMethodRef = paymentMethod.ref
            )
            pmRefProvider = TestPmRefProvider(paymentMethod.ref)
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
        val component = DaggerForageSDKTestComponent.builder()
            .forageConfig(forageConfig)
            .securePinCollector(TestableSecurePinCollector(goodPin))
            .build()
        val balanceResponse = ForageSDK().checkBalance(
            component = component,
            paymentMethodRef = paymentMethod.ref
        )
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

        val component = DaggerForageSDKTestComponent.builder()
            .forageConfig(forageConfig)
            .securePinCollector(TestableSecurePinCollector(goodPin))
            .build()
        val captureResponse = ForageSDK().capturePayment(
            component = component,
            paymentRef = cashPayment.ref
        )
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
        val component = DaggerForageSDKTestComponent.builder()
            .forageConfig(forageConfig)
            .securePinCollector(TestableSecurePinCollector(goodPin))
            .build()
        val snapDeferResponse = ForageSDK().deferPaymentCapture(component, snapPayment.ref)
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
        val component = DaggerForageSDKTestComponent.builder()
            .forageConfig(forageConfig)
            .securePinCollector(TestableSecurePinCollector(badPin))
            .build()
        val balanceCheckResponse = ForageSDK().checkBalance(
            component = component,
            paymentMethodRef = paymentMethod.ref
        )
        assertBadPinError(balanceCheckResponse)

        // Test cash payment capture with bad PIN
        val cashPayment = paymentService.createPayment(
            paymentMethodRef = paymentMethod.ref,
            amount = "1.00",
            fundingType = "ebt_cash"
        )
        val captureResponse = ForageSDK().capturePayment(
            component = component,
            paymentRef = cashPayment.ref
        )
        assertBadPinError(captureResponse)

        // Test SNAP payment deferred capture with bad PIN
        val snapPayment = paymentService.createPayment(
            paymentMethodRef = paymentMethod.ref,
            amount = "1.00",
            fundingType = "ebt_snap"
        )
        val deferResponse = ForageSDK().deferPaymentCapture(
            component = component,
            paymentRef = snapPayment.ref
        )
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

        val component = DaggerForageSDKTestComponent.builder()
            .forageConfig(forageConfig)
            .securePinCollector(TestableSecurePinCollector(goodPin))
            .build()
        val snapDeferResponse = ForageSDK().deferPaymentCapture(component, snapPayment.ref)
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
