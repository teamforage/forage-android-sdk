package com.joinforage.forage.android.pos

import com.joinforage.forage.android.CapturePaymentParams
import com.joinforage.forage.android.CheckBalanceParams
import com.joinforage.forage.android.DeferPaymentCaptureParams
import com.joinforage.forage.android.ForageSDK
import com.joinforage.forage.android.TokenizeEBTCardParams
import com.joinforage.forage.android.VaultType
import com.joinforage.forage.android.core.telemetry.Log
import com.joinforage.forage.android.fixtures.givenContentId
import com.joinforage.forage.android.fixtures.givenEncryptionKey
import com.joinforage.forage.android.fixtures.givenPaymentMethod
import com.joinforage.forage.android.fixtures.givenPaymentMethodRef
import com.joinforage.forage.android.fixtures.givenPaymentRef
import com.joinforage.forage.android.fixtures.returnsEncryptionKeySuccessfully
import com.joinforage.forage.android.fixtures.returnsMessageCompletedSuccessfully
import com.joinforage.forage.android.fixtures.returnsMissingCustomerIdPaymentMethodSuccessfully
import com.joinforage.forage.android.fixtures.returnsPayment
import com.joinforage.forage.android.fixtures.returnsPaymentMethod
import com.joinforage.forage.android.fixtures.returnsPaymentMethodSuccessfully
import com.joinforage.forage.android.fixtures.returnsPaymentMethodWithBalance
import com.joinforage.forage.android.mock.MockLogger
import com.joinforage.forage.android.mock.MockServiceFactory
import com.joinforage.forage.android.mock.MockVaultSubmitter
import com.joinforage.forage.android.mock.getVaultMessageResponse
import com.joinforage.forage.android.mock.mockSuccessfulPosDeferredRefund
import com.joinforage.forage.android.mock.mockSuccessfulPosRefund
import com.joinforage.forage.android.model.Card
import com.joinforage.forage.android.model.PaymentMethod
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import com.joinforage.forage.android.ui.ForageConfig
import com.joinforage.forage.android.ui.ForagePANEditText
import com.joinforage.forage.android.ui.ForagePINEditText
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import me.jorgecastillo.hiroaki.Method
import me.jorgecastillo.hiroaki.headers
import me.jorgecastillo.hiroaki.internal.MockServerSuite
import me.jorgecastillo.hiroaki.matchers.times
import me.jorgecastillo.hiroaki.models.json
import me.jorgecastillo.hiroaki.verify
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ForageTerminalSDKTest : MockServerSuite() {
    private lateinit var mockForagePanEditText: ForagePANEditText
    private lateinit var mockForagePinEditText: ForagePINEditText
    private lateinit var terminalSdk: ForageTerminalSDK
    private lateinit var mockForageSdk: ForageSDK
    private lateinit var mockLogger: MockLogger
    private val expectedData = MockServiceFactory.ExpectedData
    private lateinit var vaultSubmitter: MockVaultSubmitter

    @Before
    fun setUp() {
        super.setup()

        mockLogger = MockLogger()
        vaultSubmitter = MockVaultSubmitter(VaultType.FORAGE_VAULT_TYPE)
        // Use Mockito judiciously (mainly for mocking views)!
        // Opt for dependency injection and inheritance over Mockito
        mockForagePanEditText = mock(ForagePANEditText::class.java)
        mockForagePinEditText = mock(ForagePINEditText::class.java)
        mockForageSdk = mock(ForageSDK::class.java)

        val forageConfig = ForageConfig(
            merchantId = expectedData.merchantId,
            sessionToken = expectedData.sessionToken
        )
        `when`(mockForagePinEditText.getForageConfig()).thenReturn(forageConfig)
        `when`(mockForagePinEditText.getVaultType()).thenReturn(vaultSubmitter.getVaultType())

        terminalSdk = createMockTerminalSdk()
    }

    @Test
    fun `POS should send the correct headers + body to tokenize the card`() = runTest {
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
    fun `POS tokenize EBT card with Track 2 data successfully`() = runTest {
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
                card = Card.EbtCard(
                    last4 = "7845",
                    token = "tok_sandbox_sYiPe9Q249qQ5wQyUPP5f7",
                    usState = (response.card as Card.EbtCard).usState
                ),
                reusable = true,
                customerId = null
            )
        )
        assertFirstLoggedMessage("[POS] Tokenizing Payment Method using magnetic card swipe with Track 2 data on Terminal pos-terminal-id-123")
    }

    @Test
    fun `POS tokenize EBT card via UI-based PAN entry`() = runTest {
        `when`(
            mockForageSdk.tokenizeEBTCard(
                TokenizeEBTCardParams(
                    foragePanEditText = mockForagePanEditText
                )
            )
        ).thenReturn(
            ForageApiResponse.Success("Success")
        )

        val terminalSdk = createMockTerminalSdk(false)

        val response = terminalSdk.tokenizeCard(
            foragePanEditText = mockForagePanEditText
        )
        assertFirstLoggedMessage("[POS] Tokenizing Payment Method via UI PAN entry on Terminal pos-terminal-id-123")
        assertTrue(response is ForageApiResponse.Success)
        assertTrue((response as ForageApiResponse.Success).data == "Success")
    }

    @Test
    fun `POS EBT checkBalance should succeed`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        // Get Payment Method is called twice!
        server.givenPaymentMethodRef().returnsPaymentMethod()
        server.givenPaymentMethodRef().returnsPaymentMethodWithBalance()
        vaultSubmitter.setSubmitResponse(
            path = "/api/payment_methods/${expectedData.paymentMethodRef}/balance/",
            response = ForageApiResponse.Success(getVaultMessageResponse(expectedData.contentId))
        )
        server.givenContentId(expectedData.contentId)
            .returnsMessageCompletedSuccessfully()

        val response = executeCheckBalance()

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Success::class.java)
        val successResponse = response as ForageApiResponse.Success
        assertThat(successResponse.data).contains(expectedData.balance.cash)
        assertThat(successResponse.data).contains(expectedData.balance.snap)

        assertMetricsLog()
        val attributes = mockLogger.getMetricsLog().getAttributes()

        assertThat(attributes.getValue("response_time_ms").toString().toDouble()).isGreaterThan(0.0)
        assertThat(attributes.getValue("vault_type").toString()).isEqualTo("forage")
        assertThat(attributes.getValue("action").toString()).isEqualTo("balance")
        assertThat(attributes.getValue("event_name").toString()).isEqualTo("customer_perceived_response")
        assertThat(attributes.getValue("log_type").toString()).isEqualTo("metric")
    }

    @Test
    fun `POS checkBalance should return a failure when the VGS request fails`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        server.givenPaymentMethodRef().returnsPaymentMethod()
        val failureResponse = ForageApiResponse.Failure(listOf(ForageError(500, "unknown_server_error", "Some error message from VGS")))
        vaultSubmitter.setSubmitResponse(
            path = "/api/payment_methods/${expectedData.paymentMethodRef}/balance/",
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
        vaultSubmitter.setSubmitResponse(
            path = "/api/payment_methods/${expectedData.paymentRef}/balance/",
            response = failureResponse
        )

        executeCheckBalance()

        assertLoggedError(
            expectedMessage = "[POS] checkBalance failed for PaymentMethod 1f148fe399 on Terminal pos-terminal-id-123",
            failureResponse
        )
        assertMetricsLog()
        val attributes = mockLogger.getMetricsLog().getAttributes()
        assertThat(attributes.getValue("response_time_ms").toString().toDouble()).isGreaterThan(0.0)
        assertThat(attributes.getValue("vault_type").toString()).isEqualTo("forage")
        assertThat(attributes.getValue("action").toString()).isEqualTo("balance")
        assertThat(attributes.getValue("event_name").toString()).isEqualTo("customer_perceived_response")
        assertThat(attributes.getValue("event_outcome").toString()).isEqualTo("failure")
        assertThat(attributes.getValue("log_type").toString()).isEqualTo("metric")
        assertThat(attributes.getValue("forage_error_code").toString()).isEqualTo("unknown_server_error")
    }

    @Test
    fun `POS refundPayment succeeds`() = runTest {
        mockSuccessfulPosRefund(
            mockVaultSubmitter = vaultSubmitter,
            server = server
        )
        val response = executeRefundPayment()

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Success::class.java)
        val refundResponse = JSONObject((response as ForageApiResponse.Success).data)
        val refundRef = refundResponse.getString("ref")
        val paymentRef = refundResponse.getString("payment_ref")
        val refundAmount = refundResponse.getString("amount")

        assertThat(refundRef).isEqualTo(expectedData.refundRef)
        assertThat(paymentRef).isEqualTo(expectedData.paymentRef)
        assertThat(refundAmount).isEqualTo(expectedData.refundAmount.toString())

        assertFirstLoggedMessage(
            """
            [POS] Called refundPayment for Payment 6ae6a45ff1
            with amount: 1.23
            for reason: I feel like refunding this payment!
            on Terminal: pos-terminal-id-123
            """.trimIndent()
        )

        assertMetricsLog()
        val attributes = mockLogger.getMetricsLog().getAttributes()
        assertThat(attributes.getValue("response_time_ms").toString().toDouble()).isGreaterThan(0.0)
        assertThat(attributes.getValue("vault_type").toString()).isEqualTo("forage")
        assertThat(attributes.getValue("action").toString()).isEqualTo("refund")
        assertThat(attributes.getValue("event_name").toString()).isEqualTo("customer_perceived_response")
        assertThat(attributes.getValue("log_type").toString()).isEqualTo("metric")
    }

    @Test
    fun `POS refundPayment should return a failure when the Vault request fails`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        server.givenPaymentRef().returnsPayment()
        server.givenPaymentMethodRef().returnsPaymentMethod()

        val failureResponse = ForageApiResponse.Failure(listOf(ForageError(500, "unknown_server_error", "Some error message from VGS")))
        vaultSubmitter.setSubmitResponse(
            path = "/api/payments/${expectedData.paymentRef}/refunds/",
            response = failureResponse
        )

        executeRefundPayment()

        assertLoggedError(
            expectedMessage = "[POS] refundPayment failed for Payment ${expectedData.paymentRef} on Terminal pos-terminal-id-123",
            failureResponse
        )
        assertMetricsLog()
        val attributes = mockLogger.getMetricsLog().getAttributes()
        assertThat(attributes.getValue("response_time_ms").toString().toDouble()).isGreaterThan(0.0)
        assertThat(attributes.getValue("vault_type").toString()).isEqualTo("forage")
        assertThat(attributes.getValue("action").toString()).isEqualTo("refund")
        assertThat(attributes.getValue("event_name").toString()).isEqualTo("customer_perceived_response")
        assertThat(attributes.getValue("event_outcome").toString()).isEqualTo("failure")
        assertThat(attributes.getValue("log_type").toString()).isEqualTo("metric")
        assertThat(attributes.getValue("forage_error_code").toString()).isEqualTo("unknown_server_error")
    }

    @Test
    fun `POS capturePayment`() = runTest {
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

        val terminalSdk = createMockTerminalSdk(false)
        val params = CapturePaymentParams(
            foragePinEditText = mockForagePinEditText,
            paymentRef = "payment1234"
        )
        val response = terminalSdk.capturePayment(params)
        assertTrue(response is ForageApiResponse.Success)
        assertTrue((response as ForageApiResponse.Success).data == "Success")
    }

    @Test
    fun `POS illegal vault exception`() = runTest {
        val terminalSdk = createMockTerminalSdk()

        val expectedLogSubstring = "because the vault type is not forage"
        `when`(mockForagePinEditText.getVaultType()).thenReturn(VaultType.BT_VAULT_TYPE)
        val balanceResponse = terminalSdk.checkBalance(
            CheckBalanceParams(
                foragePinEditText = mockForagePinEditText,
                paymentMethodRef = "1f148fe399"
            )
        )

        val balanceError = (balanceResponse as ForageApiResponse.Failure).errors.first()
        assertThat(balanceError.message).contains("IllegalStateException")
        assertThat(mockLogger.errorLogs.last().getMessage()).contains(expectedLogSubstring)
        assertThat(mockLogger.errorLogs.count()).isEqualTo(1)

        val refundResponse = terminalSdk.refundPayment(
            PosRefundPaymentParams(
                foragePinEditText = mockForagePinEditText,
                paymentRef = expectedData.paymentRef,
                amount = expectedData.refundAmount,
                reason = expectedData.refundReason
            )
        )

        assertTrue(refundResponse is ForageApiResponse.Failure)
        assertThat(mockLogger.errorLogs.count()).isEqualTo(2)
        assertThat(mockLogger.errorLogs.last().getMessage()).contains(expectedLogSubstring)
    }

    @Test
    fun `POS deferPaymentCapture`() = runTest {
        `when`(
            mockForageSdk.deferPaymentCapture(
                DeferPaymentCaptureParams(
                    foragePinEditText = mockForagePinEditText,
                    paymentRef = "payment1234"
                )
            )
        ).thenReturn(
            ForageApiResponse.Success("")
        )
        val params = DeferPaymentCaptureParams(
            foragePinEditText = mockForagePinEditText,
            paymentRef = "payment1234"
        )
        val terminalSdk = createMockTerminalSdk(false)
        val response = terminalSdk.deferPaymentCapture(params)
        assertTrue(response is ForageApiResponse.Success)
        assertTrue((response as ForageApiResponse.Success).data == "")
    }

    @Test
    fun `POS deferPaymentRefund succeeds`() = runTest {
        mockSuccessfulPosDeferredRefund(
            mockVaultSubmitter = vaultSubmitter,
            server = server
        )
        val response = executeDeferPaymentRefund()

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Success::class.java)
        assertTrue((response as ForageApiResponse.Success).data == "")

        assertFirstLoggedMessage(
            """
            [POS] Called deferPaymentRefund for Payment 6ae6a45ff1
            on Terminal: pos-terminal-id-123
            """.trimIndent()
        )

        assertMetricsLog()
        val attributes = mockLogger.getMetricsLog().getAttributes()
        assertThat(attributes.getValue("response_time_ms").toString().toDouble()).isGreaterThan(0.0)
        assertThat(attributes.getValue("vault_type").toString()).isEqualTo("forage")
        assertThat(attributes.getValue("action").toString()).isEqualTo("defer_refund")
        assertThat(
            attributes.getValue("event_name").toString()
        ).isEqualTo("customer_perceived_response")
        assertThat(attributes.getValue("log_type").toString()).isEqualTo("metric")
    }

    @Test
    fun `POS deferPaymentRefund fails`() = runTest {
        server.givenEncryptionKey().returnsEncryptionKeySuccessfully()
        server.givenPaymentRef().returnsPayment()
        server.givenPaymentMethodRef().returnsPaymentMethod()
        val failureResponse = ForageApiResponse.Failure(listOf(ForageError(500, "unknown_server_error", "Some error message from VGS")))
        vaultSubmitter.setSubmitResponse(
            path = "/api/payments/${expectedData.paymentRef}/refunds/collect_pin/",
            response = failureResponse
        )
        val response = executeDeferPaymentRefund()

        assertThat(response).isExactlyInstanceOf(ForageApiResponse.Failure::class.java)

        assertLoggedError(
            expectedMessage = "[POS] deferPaymentRefund failed for Payment ${expectedData.paymentRef} on Terminal pos-terminal-id-123",
            failureResponse
        )
        assertMetricsLog()
        val attributes = mockLogger.getMetricsLog().getAttributes()
        assertThat(attributes.getValue("response_time_ms").toString().toDouble()).isGreaterThan(0.0)
        assertThat(attributes.getValue("vault_type").toString()).isEqualTo("forage")
        assertThat(attributes.getValue("action").toString()).isEqualTo("defer_refund")
        assertThat(attributes.getValue("event_name").toString()).isEqualTo("customer_perceived_response")
        assertThat(attributes.getValue("event_outcome").toString()).isEqualTo("failure")
        assertThat(attributes.getValue("log_type").toString()).isEqualTo("metric")
        assertThat(attributes.getValue("forage_error_code").toString()).isEqualTo("unknown_server_error")
    }

    private suspend fun executeTokenizeCardWithTrack2Data(
        track2Data: String,
        reusable: Boolean
    ): ForageApiResponse<String> {
        val terminalSdk = createMockTerminalSdk()
        return terminalSdk.tokenizeCard(
            PosTokenizeCardParams(
                posForageConfig = PosForageConfig(
                    merchantId = expectedData.merchantId,
                    sessionToken = expectedData.sessionToken
                ),
                track2Data = track2Data,
                reusable = reusable
            )
        )
    }

    private suspend fun executeCheckBalance(): ForageApiResponse<String> {
        val terminalSdk = createMockTerminalSdk()
        return terminalSdk.checkBalance(
            CheckBalanceParams(
                foragePinEditText = mockForagePinEditText,
                paymentMethodRef = "1f148fe399"
            )
        )
    }

    private suspend fun executeRefundPayment(): ForageApiResponse<String> {
        val terminalSdk = createMockTerminalSdk()
        return terminalSdk.refundPayment(
            PosRefundPaymentParams(
                foragePinEditText = mockForagePinEditText,
                paymentRef = expectedData.paymentRef,
                amount = expectedData.refundAmount,
                reason = expectedData.refundReason
            )
        )
    }

    private suspend fun executeDeferPaymentRefund(): ForageApiResponse<String> {
        val terminalSdk = createMockTerminalSdk()
        return terminalSdk.deferPaymentRefund(
            PosDeferPaymentRefundParams(
                foragePinEditText = mockForagePinEditText,
                paymentRef = expectedData.paymentRef
            )
        )
    }

    private fun createMockTerminalSdk(withMockServiceFactory: Boolean = true): ForageTerminalSDK {
        if (withMockServiceFactory) {
            return ForageTerminalSDK(
                posTerminalId = expectedData.posVaultRequestParams.posTerminalId,
                forageSdk = ForageSDK(),
                createLogger = { mockLogger },
                createServiceFactory = { _: String, _: String, logger: Log ->
                    MockServiceFactory(
                        mockVaultSubmitter = vaultSubmitter,
                        logger = logger,
                        server = server
                    )
                },
                initSucceeded = true
            )
        }
        return ForageTerminalSDK(
            posTerminalId = expectedData.posTerminalId,
            forageSdk = mockForageSdk,
            createLogger = { mockLogger },
            initSucceeded = true
        )
    }

    private fun assertLoggedError(expectedMessage: String, failureResponse: ForageApiResponse.Failure) {
        val firstForageError = failureResponse.errors.first()
        assertThat(mockLogger.errorLogs.first().getMessage()).contains(
            """
            $expectedMessage: Code: ${firstForageError.code}
            Message: ${firstForageError.message}
            Status Code: ${firstForageError.httpStatusCode}
            """.trimIndent()
        )
    }

    private fun assertFirstLoggedMessage(expectedMessage: String) =
        assertThat(mockLogger.infoLogs.first().getMessage()).contains(expectedMessage)

    private fun assertMetricsLog() =
        assertThat(mockLogger.infoLogs.last().getMessage()).contains(
            "[Metrics] Customer perceived response time"
        )
}
