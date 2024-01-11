package com.joinforage.forage.android.pos

import com.joinforage.forage.android.CapturePaymentParams
import com.joinforage.forage.android.CheckBalanceParams
import com.joinforage.forage.android.DeferPaymentCaptureParams
import com.joinforage.forage.android.ForageSDK
import com.joinforage.forage.android.ForageSDKInterface
import com.joinforage.forage.android.TokenizeEBTCardParams
import com.joinforage.forage.android.core.telemetry.Log
import com.joinforage.forage.android.fixtures.givenContentId
import com.joinforage.forage.android.fixtures.givenEncryptionKey
import com.joinforage.forage.android.fixtures.givenPaymentMethodRef
import com.joinforage.forage.android.fixtures.returnsEncryptionKeySuccessfully
import com.joinforage.forage.android.fixtures.returnsMessageCompletedSuccessfully
import com.joinforage.forage.android.fixtures.returnsPaymentMethod
import com.joinforage.forage.android.fixtures.returnsPaymentMethodWithBalance
import com.joinforage.forage.android.mock.ExpectedData
import com.joinforage.forage.android.mock.MockLogger
import com.joinforage.forage.android.mock.createMockCheckBalanceRepository
import com.joinforage.forage.android.mock.getVaultMessageResponse
import com.joinforage.forage.android.network.data.CheckBalanceRepository
import com.joinforage.forage.android.network.data.TestPinCollector
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import com.joinforage.forage.android.ui.ForageConfig
import com.joinforage.forage.android.ui.ForagePANEditText
import com.joinforage.forage.android.ui.ForagePINEditText
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import me.jorgecastillo.hiroaki.internal.MockServerSuite
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

internal class MockServiceFactory(
    private val mockPinCollector: TestPinCollector,
    private val server: MockWebServer,
    private val logger: Log,
    sessionToken: String,
    merchantId: String
) : ForageSDK.ServiceFactory(
    sessionToken,
    merchantId,
    logger
) {
    override fun createCheckBalanceRepository(foragePinEditText: ForagePINEditText): CheckBalanceRepository {
        return createMockCheckBalanceRepository(
            pinCollector = mockPinCollector,
            server = server,
            logger = logger
        )
    }
}

@RunWith(RobolectricTestRunner::class)
class ForageTerminalSDKTest : MockServerSuite() {
    private lateinit var mockForagePanEditText: ForagePANEditText
    private lateinit var mockForagePinEditText: ForagePINEditText
    private lateinit var terminalSdk: ForageTerminalSDK
    private lateinit var mockForageSdk: ForageSDK
    private lateinit var mockLogger: MockLogger
    private val testData = ExpectedData()
    private lateinit var mockPinCollector: TestPinCollector

    @Before
    fun setUp() {
        super.setup()
        // Use Mockito judiciously (mainly for mocking views)!
        // Opt for dependency injection and inheritance over Mockito
        mockForagePanEditText = mock(ForagePANEditText::class.java)
        mockForagePinEditText = mock(ForagePINEditText::class.java)

        mockLogger = MockLogger()

        mockPinCollector = TestPinCollector()
        `when`(mockForagePinEditText.getForageConfig()).thenReturn(
            ForageConfig(
                merchantId = testData.merchantAccount,
                sessionToken = testData.bearerToken
            )
        )
        `when`(mockForagePinEditText.getCollector(anyString())).thenReturn(mockPinCollector)

        mockForageSdk = mock(ForageSDK::class.java)

        terminalSdk = ForageTerminalSDK(
            posTerminalId = "1234",
            forageSdk = mockForageSdk,
            createLogger = { mockLogger }
        )
    }

    @Test
    fun `tokenize EBT card via UI-based PAN entry`() = runTest {
        `when`(
            mockForageSdk.tokenizeEBTCard(
                TokenizeEBTCardParams(
                    foragePanEditText = mockForagePanEditText
                )
            )
        ).thenReturn(
            ForageApiResponse.Success("Success")
        )

        val response = terminalSdk.tokenizeCard(
            foragePanEditText = mockForagePanEditText
        )

        val loggedMessage = mockLogger.infoLogs[0].getMessage()
        assertEquals(loggedMessage, "[POS] Tokenizing Payment Method via UI PAN entry")

        assertTrue(response is ForageApiResponse.Success)
        assertTrue((response as ForageApiResponse.Success).data == "Success")
    }

    @Test
    fun `POS EBT checkBalance should succeed`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        // Get Payment Method is called twice!
        server.givenPaymentMethodRef().returnsPaymentMethod()
        server.givenPaymentMethodRef().returnsPaymentMethodWithBalance()
        mockPinCollector.setBalanceCheckResponse(
            paymentMethodRef = testData.paymentMethodRef,
            vaultRequestParams = testData.posVaultRequestParams,
            ForageApiResponse.Success(getVaultMessageResponse(testData.contentId))
        )
        server.givenContentId(testData.contentId)
            .returnsMessageCompletedSuccessfully()

        val response = executeCheckBalance()

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Success::class.java)
        val successResponse = response as ForageApiResponse.Success
        assertThat(successResponse.data).contains(testData.balance.cash)
        assertThat(successResponse.data).contains(testData.balance.snap)

        // assert telemetry events are reported as expected!
        val loggedMessage = mockLogger.infoLogs[0].getMessage()
        assertEquals(loggedMessage, "[POS] Called checkBalance for PaymentMethod 1f148fe399")

        val metricsLog = mockLogger.infoLogs.last()
        assert(metricsLog.getMessage().contains("[Metrics] Customer perceived response time"))
        val attributes = metricsLog.getAttributes()

        assertThat(attributes.getValue("response_time_ms").toString().toDouble()).isGreaterThan(0.0)
        assertThat(attributes.getValue("vault_type").toString()).isEqualTo("vgs")
        assertThat(attributes.getValue("action").toString()).isEqualTo("balance")
        assertThat(attributes.getValue("event_name").toString()).isEqualTo("customer_perceived_response")
        assertThat(attributes.getValue("log_type").toString()).isEqualTo("metric")
    }

    @Test
    fun `POS checkBalance should return a failure when the VGS request fails`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        server.givenPaymentMethodRef().returnsPaymentMethod()
        val failureResponse = ForageApiResponse.Failure(listOf(ForageError(500, "unknown_server_error", "Some error message from VGS")))
        mockPinCollector.setBalanceCheckResponse(
            paymentMethodRef = testData.paymentMethodRef,
            vaultRequestParams = testData.posVaultRequestParams,
            response = failureResponse
        )

        val response = executeCheckBalance()

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)
        assertThat(response).isEqualTo(failureResponse)
    }

    fun `POS checkBalance should report metrics upon failure`() = runTest() {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        server.givenPaymentMethodRef().returnsPaymentMethod()
        val failureResponse = ForageApiResponse.Failure(listOf(ForageError(500, "unknown_server_error", "Some error message from VGS")))
        mockPinCollector.setBalanceCheckResponse(
            paymentMethodRef = testData.paymentMethodRef,
            vaultRequestParams = testData.posVaultRequestParams,
            response = failureResponse
        )

        executeCheckBalance()

        // assert telemetry events are reported as expected!
        val loggedMessage = mockLogger.errorLogs[0].getMessage()
        assertEquals(
            loggedMessage,
            """
        [POS] checkBalance failed for PaymentMethod 1f148fe399 on Terminal pos-terminal-id-123: Code: unknown_server_error
        Message: Some error message from VGS
        Status Code: 500
        Error Details (below):
        null
            """.trimIndent()
        )

        val metricsLog = mockLogger.infoLogs.last()
        assert(metricsLog.getMessage().contains("[Metrics] Customer perceived response time"))
        val attributes = metricsLog.getAttributes()

        assertThat(attributes.getValue("response_time_ms").toString().toDouble()).isGreaterThan(0.0)
        assertThat(attributes.getValue("vault_type").toString()).isEqualTo("vgs")
        assertThat(attributes.getValue("action").toString()).isEqualTo("balance")
        assertThat(attributes.getValue("event_name").toString()).isEqualTo("customer_perceived_response")
        assertThat(attributes.getValue("event_outcome").toString()).isEqualTo("failure")
        assertThat(attributes.getValue("log_type").toString()).isEqualTo("metric")
        assertThat(attributes.getValue("forage_error_code").toString()).isEqualTo("unknown_server_error")
    }

    @Test
    fun testCapturePayment() = runTest {
        `when`(
            mockForageSdk.capturePayment(
                CapturePaymentParams(
                    foragePinEditText = mockForagePinEditText,
                    paymentRef = "payment1234"
                )
            )
        ).thenReturn(
            ForageApiResponse.Success("Success")
        )

        val params = CapturePaymentParams(
            foragePinEditText = mockForagePinEditText,
            paymentRef = "payment1234"
        )
        val response = terminalSdk.capturePayment(params)
        assertTrue(response is ForageApiResponse.Success)
        assertTrue((response as ForageApiResponse.Success).data == "Success")
    }

    @Test
    fun testDeferPaymentCapture() = runTest {
        `when`(
            mockForageSdk.deferPaymentCapture(
                DeferPaymentCaptureParams(
                    foragePinEditText = mockForagePinEditText,
                    paymentRef = "payment1234"
                )
            )
        ).thenReturn(
            ForageApiResponse.Success("Success")
        )
        val params = DeferPaymentCaptureParams(
            foragePinEditText = mockForagePinEditText,
            paymentRef = "payment1234"
        )
        val response = terminalSdk.deferPaymentCapture(params)
        assertTrue(response is ForageApiResponse.Success)
        assertTrue((response as ForageApiResponse.Success).data == "Success")
    }

    private suspend fun executeCheckBalance(): ForageApiResponse<String> {
        val terminalSdk = ForageTerminalSDK(
            posTerminalId = testData.posVaultRequestParams.posTerminalId,
            forageSdk = ForageSDK(),
            createLogger = { mockLogger },
            createServiceFactory = { sessionToken: String, merchantId: String, logger: Log ->
                MockServiceFactory(
                    mockPinCollector = mockPinCollector,
                    server = server,
                    logger = logger,
                    sessionToken = sessionToken,
                    merchantId = merchantId
                )
            }
        )
        return terminalSdk.checkBalance(
            CheckBalanceParams(
                foragePinEditText = mockForagePinEditText,
                paymentMethodRef = "1f148fe399"
            )
        )
    }
}
