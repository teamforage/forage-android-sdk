package com.joinforage.forage.android.pos.integration

import com.joinforage.forage.android.core.forageapi.getAccessToken
import com.joinforage.forage.android.core.forageapi.getSessionToken
import com.joinforage.forage.android.core.logger.LoggableAttributes
import com.joinforage.forage.android.core.services.EnvConfig
import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.engine.ForageErrorResponseException
import com.joinforage.forage.android.core.services.forageapi.engine.IHttpEngine
import com.joinforage.forage.android.core.services.forageapi.network.FailedToReadKsnFileError
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.network.IncompletePinError
import com.joinforage.forage.android.core.services.forageapi.network.UnknownErrorApiResponse
import com.joinforage.forage.android.core.services.forageapi.network.UnknownTimeoutErrorResponse
import com.joinforage.forage.android.core.services.forageapi.network.error.ForageError
import com.joinforage.forage.android.core.services.forageapi.payment.SubmissionTestCaseFactory
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.EbtCard
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethod
import com.joinforage.forage.android.core.services.forageapi.requests.BaseApiRequest
import com.joinforage.forage.android.core.services.generateTraceId
import com.joinforage.forage.android.core.services.telemetry.Loggable
import com.joinforage.forage.android.core.services.telemetry.MetricOutcome
import com.joinforage.forage.android.core.services.telemetry.UserAction
import com.joinforage.forage.android.pos.TestFailedRequestHttpEngine
import com.joinforage.forage.android.pos.forageapi.paymentmethod.dressUpPanAsTrack2
import com.joinforage.forage.android.pos.logger.PosLoggableAttributesFactory
import com.joinforage.forage.android.pos.services.emvchip.MagSwipeInteraction
import com.joinforage.forage.android.pos.services.encryption.storage.IPersistentStorage
import com.joinforage.forage.android.pos.services.encryption.storage.InMemoryKeyRegisters
import com.joinforage.forage.android.pos.services.encryption.storage.KsnFileManager
import com.joinforage.forage.android.pos.services.encryption.storage.StringKsnManager
import com.joinforage.forage.android.pos.services.network.error.PosErrorResponseParser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.BeforeClass
import org.junit.Test

class PinSubmissionTest {

    companion object {
        private val merchantRef = "e6b746712a" // "c67e8569c1"
        private val posTerminalId = "HeadlessAndroidIntegrationTests"
        private val username = "o3vJFaHmO3eOGLxhREmwk7GHIAD4k7E9WTOwGeUP"
        private val password = "BrqSz3vDhb98nwW2wJ7OpZtx5eQYTKuJGhAD4BxSKKk0yvBNjBy6yVArn1wpFQJX618yo2oA4PUCyRWJj4SflMuhPGSGj4kaJXK158uMJvOdtT5CU4uVyeopfpx3ooDx"
        private val pan = "6777 7777 7777 7777".filter { it.isDigit() }
        private val interaction = MagSwipeInteraction(dressUpPanAsTrack2(pan))
        private val badPIN = "1234"
        private val validPIN = pan.takeLast(4)
        private val env = EnvConfig.Dev
        private val traceId = generateTraceId()
        private val ksnFileManager = StringKsnManager()
        private val keyRegisters = InMemoryKeyRegisters()
        private val paymentMethodRef = "fake_payment_method_ref"
        private val paymentRef = "fake_payment_ref"

        private lateinit var forageConfig: ForageConfig
        private lateinit var successAttrs: LoggableAttributes
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

            // Initialize the submission test case factory
            submissionTestCaseFactory = SubmissionTestCaseFactory(
                validPIN = validPIN,
                forageConfig = forageConfig,
                ksnFileManager = ksnFileManager,
                keyRegisters = keyRegisters,
                paymentRef = paymentRef,
                posTerminalId = posTerminalId,
                interaction = interaction,
                traceId = traceId
            )

            successAttrs = PosLoggableAttributesFactory(
                forageConfig = forageConfig,
                traceId = traceId,
                posTerminalId = posTerminalId
            )(UserAction.BALANCE)

            failureAttrs = PosLoggableAttributesFactory(
                forageConfig = forageConfig,
                traceId = traceId,
                posTerminalId = posTerminalId
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
        failureAttrs = PosLoggableAttributesFactory(
            forageConfig = forageConfig,
            traceId = traceId,
            posTerminalId = posTerminalId
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
                    PosErrorResponseParser().toForageError(200, malformedResponse.toString())
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

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testCannotReadKsnFileException() = runTest {
        val exception = RuntimeException("Failed to read KSN file")
        val paymentMethodWithIncompleteToken = PaymentMethod(
            ref = paymentMethodRef,
            type = "ebt",
            customerId = "dummy_customer_id",
            balance = null,
            card = EbtCard(
                last4 = "7777",
                fingerprint = "dummy_fingerprint",
                token = "vgs_token,basis_theory_token,forage_token", // Missing Forage token
                number = "6777777777777777",
                usState = null
            ),
            reusable = true
        )
        val (
            submission,
            logger,
            collector
        ) = submissionTestCaseFactory.newBalanceCheckSubmission(
            ksnFileManager = object : KsnFileManager(
                object : IPersistentStorage {
                    override fun exists(): Boolean {
                        return true
                    }

                    override fun write(content: String) {
                        // no-op for test
                    }

                    override fun read(): List<String> {
                        throw exception
                    }
                }
            ) {}
        )

        val response = submission.submit()
        assertThat(response).isEqualTo(FailedToReadKsnFileError)

        // Verify the collector was cleared
        assertThat(collector.wasCleared).isTrue

        // Verify the logs show the KSN file read failure
        val expectedLogs = listOf(
            Loggable.Info("[balance]", "[START] Submit Attempt", failureAttrs.logAttrs.noPM),
            Loggable.Error(
                "[balance]",
                "[END] Submission failed.\n\nProblem reading KSN file",
                KsnFileManager.CannotReadKsnFileException(exception),
                failureAttrs.logAttrs.all
            )
        )
        assertThat(logger.logs).isEqualTo(expectedLogs)
    }
}
