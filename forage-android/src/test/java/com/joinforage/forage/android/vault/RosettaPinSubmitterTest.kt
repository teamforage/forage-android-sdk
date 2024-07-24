package com.joinforage.forage.android.vault

import android.text.Editable
import android.widget.EditText
import com.joinforage.forage.android.core.services.EnvConfig
import com.joinforage.forage.android.core.services.ForageConstants
import com.joinforage.forage.android.core.services.addPathSegmentsSafe
import com.joinforage.forage.android.core.services.addTrailingSlash
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.EbtCard
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethod
import com.joinforage.forage.android.core.services.telemetry.UserAction
import com.joinforage.forage.android.core.services.vault.SecurePinCollector
import com.joinforage.forage.android.core.services.vault.VaultProxyRequest
import com.joinforage.forage.android.core.services.vault.VaultSubmitterParams
import com.joinforage.forage.android.ecom.services.vault.forage.RosettaPinSubmitter
import com.joinforage.forage.android.fixtures.givenRosettaPaymentCaptureRequest
import com.joinforage.forage.android.fixtures.returnsMalformedError
import com.joinforage.forage.android.fixtures.returnsPayment
import com.joinforage.forage.android.fixtures.returnsRosettaError
import com.joinforage.forage.android.fixtures.returnsSendToProxy
import com.joinforage.forage.android.fixtures.returnsUnauthorized
import com.joinforage.forage.android.mock.MockLogger
import com.joinforage.forage.android.mock.MockServiceFactory
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import me.jorgecastillo.hiroaki.Method
import me.jorgecastillo.hiroaki.headers
import me.jorgecastillo.hiroaki.internal.MockServerSuite
import me.jorgecastillo.hiroaki.matchers.times
import me.jorgecastillo.hiroaki.models.json
import me.jorgecastillo.hiroaki.verify
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class RosettaPinSubmitterTest() : MockServerSuite() {
    private lateinit var mockLogger: MockLogger
    private lateinit var submitter: RosettaPinSubmitter

    @Before
    fun setUp() {
        super.setup()

        mockLogger = MockLogger()

        // Use Mockito judiciously (mainly for mocking views)!
        // Opt for dependency injection and inheritance over Mockito, when possible

        // Mock the PIN value!
        val mockEditText = mock(EditText::class.java)
        val mockEditable = mock(Editable::class.java)
        `when`(mockEditable.toString()).thenReturn("1234")
        `when`(mockEditText.text).thenReturn(mockEditable)

        val mockCollector = mock(SecurePinCollector::class.java)

        submitter = RosettaPinSubmitter(
            editText = mockEditText,
            collector = mockCollector,
            logger = mockLogger,
            envConfig = EnvConfig.Local,
            // Ensure we don't make any LIVE requests!!!
            // Emulates the real vaultUrlBuilder, but using the empty test URL
            vaultUrlBuilder = { path ->
                MockServiceFactory.createEmptyUrl(server).toHttpUrlOrNull()!!.newBuilder()
                    .addPathSegment("proxy")
                    .addPathSegmentsSafe(path)
                    .addTrailingSlash()
                    .build()
            }
        )
    }

    @Test
    fun `Forage request receives expected params`() = runTest {
        val paymentRef = "abcdSuccess123"
        server.givenRosettaPaymentCaptureRequest(paymentRef).returnsPayment()

        executeSubmit(paymentRef)

        server.verify("proxy/api/payments/$paymentRef/capture").called(
            times = times(1),
            method = Method.POST,
            headers = headers(
                "Authorization" to "Bearer ${mockData.sessionToken}",
                "API-VERSION" to "default",
                "Content-Type" to "application/json; charset=utf-8",
                "IDEMPOTENCY-KEY" to MOCK_IDEMPOTENCY_KEY,
                "Merchant-Account" to mockData.merchantId,
                "x-datadog-trace-id" to MOCK_TRACE_ID
            ),
            jsonBody = json {
                "pin" / "1234"
                "card_number_token" / "tok_rosetta_abcde123"
            }
        )
    }

    @Test
    fun `getVaultToken returns null or empty String when there is no Rosetta token`() = runTest {
        val paymentMethodMissingThirdToken = buildMockPaymentMethod("tok_sandbox_sYiPe9Q249qQ5wQyUPP5f7,basis-theory-token")
        val resultMissingThirdToken = submitter.getVaultToken(paymentMethodMissingThirdToken)

        val paymentMethodEmptyToken = buildMockPaymentMethod("tok_sandbox_sYiPe9Q249qQ5wQyUPP5f7,basis-theory-token,")
        val resultEmptyToken = submitter.getVaultToken(paymentMethodEmptyToken)

        assertNull(resultMissingThirdToken)
        assertEquals("", resultEmptyToken)
    }

    @Test
    fun `getVaultToken returns the Rosetta token when it is present`() = runTest {
        val mockRosettaToken = "tok_rosetta_abcde123"
        val paymentMethodWithRosettaToken1 = buildMockPaymentMethod(",,$mockRosettaToken")
        val rosettaToken1 = submitter.getVaultToken(paymentMethodWithRosettaToken1)

        val paymentMethodWithRosettaToken2 = buildMockPaymentMethod("tok_sandbox_sYiPe9Q249qQ5wQyUPP5f7,basis-theory-token,$mockRosettaToken")
        val rosettaToken2 = submitter.getVaultToken(paymentMethodWithRosettaToken2)

        val paymentMethodWithRosettaToken3 = buildMockPaymentMethod("tok_sandbox_sYiPe9Q249qQ5wQyUPP5f7,basis-theory-token,$mockRosettaToken,some_new_token")
        val rosettaToken3 = submitter.getVaultToken(paymentMethodWithRosettaToken3)

        assertEquals(mockRosettaToken, rosettaToken1)
        assertEquals(mockRosettaToken, rosettaToken2)
        assertEquals(mockRosettaToken, rosettaToken3)
    }

    @Test
    fun `submitProxyRequest with valid input should return success`() = runTest {
        val paymentRef = "abcdefgSuccess123"

        // This can be replace with PaymentModel response instead of Message response
        // when we introduce synchronous payment captures in the android-sdk
        server.givenRosettaPaymentCaptureRequest(paymentRef).returnsSendToProxy()

        val result = executeSubmit(paymentRef)
        assertTrue(result is ForageApiResponse.Success)

        val actualJsonObject = JSONObject((result as ForageApiResponse.Success).data)

        assertEquals("[forage] Received successful response from forage", mockLogger.infoLogs.last().getMessage())
        assertEquals("45639248-03f2-498d-8aa8-9ebd1c60ee65", actualJsonObject.getString("content_id"))
        assertEquals("sent_to_proxy", actualJsonObject.getString("status"))
        assertEquals(false, actualJsonObject.getBoolean("failed"))
    }

    @Test
    fun `Rosetta returns a vault error`() = runTest {
        val paymentRef = "xyzVaultError123"
        server.givenRosettaPaymentCaptureRequest(paymentRef).returnsRosettaError()

        val result = executeSubmit(paymentRef)
        assertTrue(result is ForageApiResponse.Failure)

        val firstError = (result as ForageApiResponse.Failure).errors[0]

        assertEquals(
            """
            [forage] Received ForageError from forage: Code: auth_header_malformed
            Message: authorization header malformed
            Status Code: 401
            Error Details (below):
            null
            """.trimIndent(),
            mockLogger.errorLogs.last().getMessage()
        )
        assertEquals("authorization header malformed", firstError.message)
        assertEquals(401, firstError.httpStatusCode)
        assertEquals("auth_header_malformed", firstError.code)
    }

    @Test
    fun `Rosetta returns a ForageError`() = runTest {
        val paymentRef = "xyzForageError456"
        server.givenRosettaPaymentCaptureRequest("xyzForageError456").returnsUnauthorized()

        val result = executeSubmit(paymentRef)

        assertTrue(result is ForageApiResponse.Failure)
        val firstError = (result as ForageApiResponse.Failure).errors[0]
        assertEquals(
            """
            [forage] Received ForageError from forage: Code: missing_merchant_account
            Message: No merchant account FNS number was provided.
            Status Code: 401
            Error Details (below):
            null
            """.trimIndent(),
            mockLogger.errorLogs.last().getMessage()
        )
        assertEquals("No merchant account FNS number was provided.", firstError.message)
        assertEquals(401, firstError.httpStatusCode)
        assertEquals("missing_merchant_account", firstError.code)
    }

    @Test
    fun `Rosetta responds with a malformed error`() = runTest {
        val paymentRef = "xyzMalformedError456"
        server.givenRosettaPaymentCaptureRequest(paymentRef).returnsMalformedError()

        val result = executeSubmit(paymentRef)

        assertTrue(result is ForageApiResponse.Failure)
        val firstError = (result as ForageApiResponse.Failure).errors[0]
        assertEquals(
            "Failed to send request to Forage Vault.",
            mockLogger.errorLogs.last().getMessage()
        )
        assertEquals("Unknown Server Error", firstError.message)
        assertEquals(500, firstError.httpStatusCode)
        assertEquals("unknown_server_error", firstError.code)
    }

    // The paths dictate which mock server response to expect
    // The paymentRef is used to determine the path
    private suspend fun executeSubmit(paymentRef: String): ForageApiResponse<String> {
        return submitter.submitProxyRequest(buildMockVaultProxyRequest(paymentRef))
    }

    companion object {
        private val mockData = MockServiceFactory.ExpectedData
        private const val MOCK_IDEMPOTENCY_KEY = "idempotency-abcdef123"

        /**
         * Comes from [MockLogger.getTraceIdValue]
         */
        private const val MOCK_TRACE_ID = "11223344"
        private const val MOCK_ROSETTA_CARD_TOKEN = "tok_rosetta_abcde123"

        private fun buildMockPaymentCapturePath(paymentRef: String) = "/api/payments/$paymentRef/capture/"

        // The paths dictate which mock server response to expect
        private fun buildMockVaultProxyRequest(paymentRef: String): VaultProxyRequest {
            val path = buildMockPaymentCapturePath(paymentRef)

            return VaultProxyRequest.emptyRequest()
                /**
                 * we emulate the behavior of the x-key header being set by the
                 * [com.joinforage.forage.android.core.services.vault.AbstractVaultSubmitter]
                 * But the x-key header is not applicable to Rosetta client
                 * and is omitted via [RosettaPinSubmitter.parseEncryptionKey]
                 */
                .setHeader(ForageConstants.Headers.X_KEY, "22320ce0-1a3c-4c64-970c-51ed7db34548")
                .setHeader(ForageConstants.Headers.MERCHANT_ACCOUNT, mockData.merchantId)
                .setHeader(ForageConstants.Headers.IDEMPOTENCY_KEY, MOCK_IDEMPOTENCY_KEY)
                .setHeader(ForageConstants.Headers.TRACE_ID, MOCK_TRACE_ID)
                .setToken(MOCK_ROSETTA_CARD_TOKEN)
                .setPath(path)
                .setHeader(ForageConstants.Headers.CONTENT_TYPE, "application/json")
                .setParams(buildMockVaultParams(path))
        }

        private fun buildMockVaultParams(path: String) = VaultSubmitterParams(
            encryptionKeys = MockServiceFactory.ExpectedData.mockEncryptionKeys,
            idempotencyKey = MOCK_IDEMPOTENCY_KEY,
            merchantId = "1234567",
            path = "/api/payments/abcdefg123/capture/",
            paymentMethod = MockServiceFactory.ExpectedData.mockPaymentMethod,
            userAction = UserAction.CAPTURE,
            sessionToken = mockData.sessionToken
        )

        private fun buildMockPaymentMethod(cardNumberToken: String) = PaymentMethod(
            ref = "abcde123",
            type = "ebt",
            balance = null,
            card = EbtCard(
                last4 = "7845",
                fingerprint = "abdde-fingerprint",
                token = cardNumberToken
            )
        )
    }
}
