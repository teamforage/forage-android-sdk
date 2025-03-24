package com.joinforage.forage.android.ecom.integration

import com.joinforage.forage.android.core.TestFailedRequestHttpEngine
import com.joinforage.forage.android.core.forageapi.getAccessToken
import com.joinforage.forage.android.core.forageapi.getSessionToken
import com.joinforage.forage.android.core.forageapi.payment.TestPaymentService
import com.joinforage.forage.android.core.logger.LoggableAttributes
import com.joinforage.forage.android.core.services.EnvConfig
import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.engine.ForageErrorResponseException
import com.joinforage.forage.android.core.services.forageapi.engine.IHttpEngine
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.network.IncompletePinError
import com.joinforage.forage.android.core.services.forageapi.network.UnknownErrorApiResponse
import com.joinforage.forage.android.core.services.forageapi.network.UnknownTimeoutErrorResponse
import com.joinforage.forage.android.core.services.forageapi.network.error.ForageError
import com.joinforage.forage.android.core.services.forageapi.payment.Payment
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethod
import com.joinforage.forage.android.core.services.forageapi.requests.BaseApiRequest
import com.joinforage.forage.android.core.services.generateTraceId
import com.joinforage.forage.android.core.services.telemetry.Loggable
import com.joinforage.forage.android.core.services.telemetry.MetricOutcome
import com.joinforage.forage.android.core.services.telemetry.UserAction
import com.joinforage.forage.android.core.services.vault.IPmRefProvider
import com.joinforage.forage.android.ecom.logger.EcomLoggableAttributesFactory
import com.joinforage.forage.android.ecom.services.forageapi.engine.EcomOkHttpEngine
import com.joinforage.forage.android.ecom.services.forageapi.paymentmethod.PaymentMethodService
import com.joinforage.forage.android.ecom.services.network.error.EcomErrorResponseParser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test

@Ignore("Require server credentials and this is a public repo")
class PinSubmissionTest {

    private class TestPmRefProvider(private val pmRef: String) : IPmRefProvider {
        override suspend fun getPaymentMethodRef(): String = pmRef
    }

    companion object {
        private val merchantRef = "your_merchant_id"
        private val username = "your_client_app_id"
        private val password = "your_client_app_secret"
        private val pan = "6777 7777 7777 7777".filter { it.isDigit() }
        private val badPIN = "1234"
        private val goodPIN = pan.takeLast(4)
        private val env = EnvConfig.Dev
        private val httpEngine = EcomOkHttpEngine()
        private val traceId = generateTraceId()

        private lateinit var pmRefProvider: TestPmRefProvider
        private lateinit var forageConfig: ForageConfig
        private lateinit var paymentMethodService: PaymentMethodService
        private lateinit var paymentService: TestPaymentService
        private lateinit var paymentMethod: PaymentMethod
        private lateinit var payment: Payment
        private lateinit var successAttrs: LoggableAttributes
        private lateinit var failureAttrs: LoggableAttributes
        private lateinit var accessToken: String
        private lateinit var submissionTestCaseFactory: SubmissionTestCaseFactory

        private val paymentMethodRef = "fake_payment_method_ref"
        private val paymentRef = "fake_payment_ref"

        @BeforeClass
        @JvmStatic
        fun setupClass() = runBlocking {
            println("The Trace ID for this test run is: $traceId")
            accessToken = getAccessToken(username, password, env)
            val sessionToken = getSessionToken(accessToken, merchantRef)
            forageConfig = ForageConfig(merchantRef, sessionToken)
            paymentMethodService = PaymentMethodService(forageConfig, traceId, httpEngine)
            paymentService = TestPaymentService(ForageConfig(merchantRef, accessToken), traceId, httpEngine)

            // Initialize the submission test case factory
            submissionTestCaseFactory = SubmissionTestCaseFactory(
                pin = goodPIN,
                forageConfig = forageConfig,
                paymentMethodService = paymentMethodService,
                paymentService = paymentService,
                paymentRef = paymentRef,
                traceId = traceId
            )

            successAttrs = EcomLoggableAttributesFactory(
                forageConfig = forageConfig,
                traceId = traceId,
                paymentMethodRef = paymentMethodRef
            )(UserAction.BALANCE)

            failureAttrs = EcomLoggableAttributesFactory(
                forageConfig = forageConfig,
                traceId = traceId,
                paymentMethodRef = paymentMethodRef
            )(UserAction.BALANCE, 500, MetricOutcome.FAILURE)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testHappyPath() = runTest {
        val (
            submission,
            logger,
            collector
        ) = submissionTestCaseFactory.newPinSubmissionAttempt()
        val response = submission.submit()
        assertThat(response).isInstanceOf(ForageApiResponse.Success::class.java)

        assertThat(collector.wasCleared).isTrue

        val expectedLogs = listOf(
            Loggable.Info("[balance]", "[START] Submit Attempt", successAttrs.logAttrs.noPM),
            Loggable.Info("[balance]", "[END] Submission succeeded!", successAttrs.logAttrs.all),
            Loggable.Metric("[balance]", "Outcome recorded!", successAttrs.metricAttrs.vaultRes),
            Loggable.Metric("[balance]", "Outcome recorded!", successAttrs.metricAttrs.cusPercep)
        )
        assertThat(logger.logs).isEqualTo(expectedLogs)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testUserIncompletePin() = runTest {
        val (
            submission,
            logger,
            collector
        ) = submissionTestCaseFactory.newPinSubmissionAttempt(isComplete = false)

        val response = (submission.submit() as ForageApiResponse.Failure)
        assertThat(response).isEqualTo(IncompletePinError)

        assertThat(collector.wasCleared).isTrue

        // Verify the logs show the failure
        val expectedLogs = listOf(
            Loggable.Info("[balance]", "[START] Submit Attempt", failureAttrs.logAttrs.noPM),
            Loggable.Warn("[balance]", "[END] Submission failed.\n\nPin is incomplete.", failureAttrs.logAttrs.all)
        )
        assertThat(logger.logs).isEqualTo(expectedLogs)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testVaultResponseFailure() = runTest {
        val forageError = ForageError(
            500,
            code = "ebt_error_55",
            message = "Invalid PIN or PIN not selected - Invalid PIN"
        )
        val (
            submission,
            logger,
            collector
        ) = submissionTestCaseFactory.newPinSubmissionAttempt(
            vaultHttpEngine = object : IHttpEngine {
                override suspend fun sendRequest(request: BaseApiRequest): String {
                    throw ForageErrorResponseException(forageError)
                }
            }
        )

        val response = submission.submit()
        val failureResponse = (response as ForageApiResponse.Failure).error
        assertThat(failureResponse).isEqualTo(forageError)

        // Verify the collector was cleared
        assertThat(collector.wasCleared).isTrue

        // Verify the logs show the vault submission failure
        failureAttrs = EcomLoggableAttributesFactory(
            forageConfig = forageConfig,
            traceId = traceId,
            paymentMethodRef = paymentMethodRef
        )(UserAction.BALANCE, forageError)
        val expectedLogs = listOf(
            Loggable.Info("[balance]", "[START] Submit Attempt", failureAttrs.logAttrs.noPM),
            Loggable.Metric("[balance]", "Outcome recorded!", failureAttrs.metricAttrs.vaultRes),
            Loggable.Metric("[balance]", "Outcome recorded!", failureAttrs.metricAttrs.cusPercep),
            Loggable.Warn(
                "[balance]",
                "[END] Submission failed.\n\nForage Proxy response is:\n\n$forageError",
                failureAttrs.logAttrs.all
            )
        )
        assertThat(logger.logs).isEqualTo(expectedLogs)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testUnknownForageFailureResponse() = runTest {
        val malformedResponse = JSONObject(
            """
            {
                "unexpected_field": "unexpected_value"
            }
            """.trimIndent()
        )

        val (
            submission,
            logger,
            collector
        ) = submissionTestCaseFactory.newPinSubmissionAttempt(
            vaultHttpEngine = object : IHttpEngine {
                override suspend fun sendRequest(request: BaseApiRequest): String {
                    EcomErrorResponseParser().toForageError(200, malformedResponse.toString())
                    return "unreachable code"
                }
            }
        )

        val response = submission.submit()
        assertThat(response).isEqualTo(UnknownErrorApiResponse)

        // Verify the collector was cleared
        assertThat(collector.wasCleared).isTrue

        // Verify the logs show the malformed response failure
        val expectedLogs = listOf(
            Loggable.Info("[balance]", "[START] Submit Attempt", failureAttrs.logAttrs.noPM),
            Loggable.Error(
                "[balance]",
                "[END] Submission failed.\n\nMalformed Forage API response is $malformedResponse",
                null,
                failureAttrs.logAttrs.all
            )
        )
        assertThat(logger.logs).isEqualTo(expectedLogs)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testSocketTimeoutHttpRequestFailed() = runTest {
        val exception = java.net.SocketTimeoutException()
        val (
            submission,
            logger,
            collector
        ) = submissionTestCaseFactory.newPinSubmissionAttempt(
            vaultHttpEngine = TestFailedRequestHttpEngine(exception)
        )

        val response = submission.submit()
        assertThat(response).isEqualTo(UnknownTimeoutErrorResponse)

        // Verify the collector was cleared
        assertThat(collector.wasCleared).isTrue

        // Verify the logs show the HTTP request failure
        val expectedLogs = listOf(
            Loggable.Info("[balance]", "[START] Submit Attempt", failureAttrs.logAttrs.noPM),
            Loggable.Error(
                "[balance]",
                "[END] Submission failed.\n\nFailed HTTP request",
                exception,
                failureAttrs.logAttrs.all
            )
        )
        assertThat(logger.logs).isEqualTo(expectedLogs)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testHttpRequestFailed() = runTest {
        val exception = RuntimeException("Network error")
        val (
            submission,
            logger,
            collector
        ) = submissionTestCaseFactory.newPinSubmissionAttempt(
            vaultHttpEngine = TestFailedRequestHttpEngine(exception)
        )

        val response = submission.submit()
        assertThat(response).isEqualTo(UnknownErrorApiResponse)

        // Verify the collector was cleared
        assertThat(collector.wasCleared).isTrue

        // Verify the logs show the HTTP request failure
        val expectedLogs = listOf(
            Loggable.Info("[balance]", "[START] Submit Attempt", failureAttrs.logAttrs.noPM),
            Loggable.Error(
                "[balance]",
                "[END] Submission failed.\n\nFailed HTTP request",
                exception,
                failureAttrs.logAttrs.all
            )
        )
        assertThat(logger.logs).isEqualTo(expectedLogs)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testUnknownExceptionFailure() = runTest {
        val unexpectedException = RuntimeException("Unexpected error occurred")
        val (
            submission,
            logger,
            collector
        ) = submissionTestCaseFactory.newPinSubmissionAttempt(
            vaultHttpEngine = object : IHttpEngine {
                override suspend fun sendRequest(request: BaseApiRequest): String {
                    throw unexpectedException
                }
            }
        )

        val response = submission.submit()
        assertThat(response).isEqualTo(UnknownErrorApiResponse)

        // Verify the collector was cleared
        assertThat(collector.wasCleared).isTrue

        // Verify the logs show the unexpected error
        val expectedLogs = listOf(
            Loggable.Info("[balance]", "[START] Submit Attempt", failureAttrs.logAttrs.noPM),
            Loggable.Error(
                "[balance]",
                "[END] Submission failed.\n\nUnknown error occurred",
                unexpectedException,
                failureAttrs.logAttrs.all
            )
        )
        assertThat(logger.logs).isEqualTo(expectedLogs)
    }
}
