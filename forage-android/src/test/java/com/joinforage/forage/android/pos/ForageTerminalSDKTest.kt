package com.joinforage.forage.android.pos

import com.joinforage.forage.android.CapturePaymentParams
import com.joinforage.forage.android.CheckBalanceParams
import com.joinforage.forage.android.DeferPaymentCaptureParams
import com.joinforage.forage.android.ForageSDK
import com.joinforage.forage.android.TokenizeEBTCardParams
import com.joinforage.forage.android.core.telemetry.Log
import com.joinforage.forage.android.fixtures.givenContentId
import com.joinforage.forage.android.fixtures.givenEncryptionKey
import com.joinforage.forage.android.fixtures.givenPaymentMethod
import com.joinforage.forage.android.fixtures.givenPaymentMethodRef
import com.joinforage.forage.android.fixtures.returnsEncryptionKeySuccessfully
import com.joinforage.forage.android.fixtures.returnsMessageCompletedSuccessfully
import com.joinforage.forage.android.fixtures.returnsMissingCustomerIdPaymentMethodSuccessfully
import com.joinforage.forage.android.fixtures.returnsPaymentMethod
import com.joinforage.forage.android.fixtures.returnsPaymentMethodSuccessfully
import com.joinforage.forage.android.fixtures.returnsPaymentMethodWithBalance
import com.joinforage.forage.android.mock.MockLogger
import com.joinforage.forage.android.mock.MockRepositoryFactory
import com.joinforage.forage.android.mock.getVaultMessageResponse
import com.joinforage.forage.android.model.Card
import com.joinforage.forage.android.model.PaymentMethod
import com.joinforage.forage.android.network.TokenizeCardService
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
import me.jorgecastillo.hiroaki.Method
import me.jorgecastillo.hiroaki.headers
import me.jorgecastillo.hiroaki.internal.MockServerSuite
import me.jorgecastillo.hiroaki.matchers.times
import me.jorgecastillo.hiroaki.models.json
import me.jorgecastillo.hiroaki.verify
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
    server: MockWebServer,
    logger: Log,
    sessionToken: String,
    merchantId: String
) : ForageSDK.ServiceFactory(
    sessionToken,
    merchantId,
    logger
) {
    private val mockRepositoryFactory = MockRepositoryFactory(
        logger = logger,
        server = server
    )

    override fun createTokenizeCardService(): TokenizeCardService {
        return mockRepositoryFactory.createTokenizeCardService()
    }

    override fun createCheckBalanceRepository(foragePinEditText: ForagePINEditText): CheckBalanceRepository {
        return mockRepositoryFactory.createCheckBalanceRepository(mockPinCollector)
    }
}

@RunWith(RobolectricTestRunner::class)
class ForageTerminalSDKTest : MockServerSuite() {
    private lateinit var mockForagePanEditText: ForagePANEditText
    private lateinit var mockForagePinEditText: ForagePINEditText
    private lateinit var terminalSdk: ForageTerminalSDK
    private lateinit var mockForageSdk: ForageSDK
    private lateinit var mockLogger: MockLogger
    private val expectedData = MockRepositoryFactory.ExpectedData
    private lateinit var mockPinCollector: TestPinCollector

    @Before
    fun setUp() {
        super.setup()

        mockPinCollector = TestPinCollector()
        mockLogger = MockLogger()

        // Use Mockito judiciously (mainly for mocking views)!
        // Opt for dependency injection and inheritance over Mockito
        mockForagePanEditText = mock(ForagePANEditText::class.java)
        mockForagePinEditText = mock(ForagePINEditText::class.java)
        mockForageSdk = mock(ForageSDK::class.java)
        `when`(mockForagePinEditText.getForageConfig()).thenReturn(
            ForageConfig(
                merchantId = expectedData.merchantId,
                sessionToken = expectedData.sessionToken
            )
        )
        `when`(mockForagePinEditText.getCollector(anyString())).thenReturn(mockPinCollector)

        terminalSdk = ForageTerminalSDK(
            posTerminalId = "1234",
            forageSdk = mockForageSdk,
            createLogger = { mockLogger }
        )
    }

    @Test
    fun `should send the correct headers + body to tokenize the card`() = runTest {
        server.givenPaymentMethod(
            PosPaymentMethodRequestBody(
                track2Data = expectedData.track2Data,
                reusable = false
            )
        ).returnsPaymentMethodSuccessfully()

        executeTokenizeCardWithTrack2Data(
            track2Data = expectedData.track2Data,
            reusable = false
        )

        server.verify("api/payment_methods/").called(
            times = times(1),
            method = Method.POST,
            headers = headers(
                "Authorization" to "Bearer ${expectedData.sessionToken}",
                "Merchant-Account" to expectedData.merchantId
            ),
            jsonBody = json {
                "type" / "ebt"
                "reusable" / false
                "card" / json {
                    "track_2_data" / expectedData.track2Data
                }
            }
        )
    }

    @Test
    fun `tokenize EBT card with Track 2 data successfully`() = runTest {
        server.givenPaymentMethod(
            PosPaymentMethodRequestBody(
                track2Data = expectedData.track2Data,
                reusable = true
            )
        ).returnsMissingCustomerIdPaymentMethodSuccessfully()

        val paymentMethodResponse = executeTokenizeCardWithTrack2Data(
            track2Data = expectedData.track2Data,
            reusable = true
        )
        assertThat(paymentMethodResponse).isExactlyInstanceOf(ForageApiResponse.Success::class.java)
        val response =
            PaymentMethod.ModelMapper.from((paymentMethodResponse as ForageApiResponse.Success).data)
        assertThat(response).isEqualTo(
            PaymentMethod(
                ref = "2f148fe399",
                type = "ebt",
                balance = null,
                card = Card(
                    last4 = "7845",
                    token = "tok_sandbox_sYiPe9Q249qQ5wQyUPP5f7"
                ),
                reusable = true,
                customerId = null
            )
        )

        val loggedMessage = mockLogger.infoLogs[0].getMessage()
        assertEquals(loggedMessage, "[POS] Tokenizing Payment Method using magnetic card swipe with Track 2 data on Terminal 1234")
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
        assertEquals(loggedMessage, "[POS] Tokenizing Payment Method via UI PAN entry on Terminal 1234")

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
            paymentMethodRef = expectedData.paymentMethodRef,
            vaultRequestParams = expectedData.posVaultRequestParams,
            ForageApiResponse.Success(getVaultMessageResponse(expectedData.contentId))
        )
        server.givenContentId(expectedData.contentId)
            .returnsMessageCompletedSuccessfully()

        val response = executeCheckBalance()

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Success::class.java)
        val successResponse = response as ForageApiResponse.Success
        assertThat(successResponse.data).contains(expectedData.balance.cash)
        assertThat(successResponse.data).contains(expectedData.balance.snap)

        // assert telemetry events are reported as expected!
        val loggedMessage = mockLogger.infoLogs[0].getMessage()
        assertEquals("[POS] Called checkBalance for PaymentMethod 1f148fe399 on Terminal pos-terminal-id-123", loggedMessage)

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
            paymentMethodRef = expectedData.paymentMethodRef,
            vaultRequestParams = expectedData.posVaultRequestParams,
            response = failureResponse
        )

        val response = executeCheckBalance()

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)
        assertThat(response).isEqualTo(failureResponse)
    }

    fun `POS checkBalance should report metrics upon failure`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        server.givenPaymentMethodRef().returnsPaymentMethod()
        val failureResponse = ForageApiResponse.Failure(listOf(ForageError(500, "unknown_server_error", "Some error message from VGS")))
        mockPinCollector.setBalanceCheckResponse(
            paymentMethodRef = expectedData.paymentMethodRef,
            vaultRequestParams = expectedData.posVaultRequestParams,
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

    private suspend fun executeTokenizeCardWithTrack2Data(
        track2Data: String,
        reusable: Boolean
    ): ForageApiResponse<String> {
        val terminalSdk = ForageTerminalSDK(
            posTerminalId = "1234",
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
        return terminalSdk.tokenizeCard(
            PosTokenizeCardParams(
                forageConfig = ForageConfig(
                    merchantId = expectedData.merchantId,
                    sessionToken = expectedData.sessionToken
                ),
                track2Data = track2Data,
                reusable = reusable
            )
        )
    }

    private suspend fun executeCheckBalance(): ForageApiResponse<String> {
        val terminalSdk = ForageTerminalSDK(
            posTerminalId = expectedData.posVaultRequestParams.posTerminalId,
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
