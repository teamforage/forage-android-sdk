package com.joinforage.forage.android.vault

import android.content.Context
import com.basistheory.android.service.BasisTheoryElements
import com.basistheory.android.service.ProxyApi
import com.basistheory.android.service.ProxyRequest
import com.basistheory.android.view.TextElement
import com.joinforage.forage.android.core.services.ForageConstants
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.vault.StopgapGlobalState
import com.joinforage.forage.android.core.services.vault.VaultProxyRequest
import com.joinforage.forage.android.core.ui.element.ForageConfig
import com.joinforage.forage.android.ecom.services.vault.bt.BasisTheoryPinSubmitter
import com.joinforage.forage.android.ecom.services.vault.bt.BasisTheoryResponse
import com.joinforage.forage.android.ecom.services.vault.bt.ProxyRequestObject
import com.joinforage.forage.android.ecom.ui.ForagePINEditText
import com.joinforage.forage.android.mock.MockLogger
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import me.jorgecastillo.hiroaki.internal.MockServerSuite
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq

class BasisTheoryPinSubmitterTest() : MockServerSuite() {
    private lateinit var mockLogger: MockLogger
    private lateinit var mockForagePinEditText: ForagePINEditText
    private lateinit var submitter: BasisTheoryPinSubmitter
    private lateinit var mockBasisTheory: BasisTheoryElements
    private lateinit var mockBasisTheoryTextElement: TextElement
    private lateinit var mockApiProxy: ProxyApi

    @Before
    fun setUp() {
        super.setup()

        mockLogger = MockLogger()

        // Use Mockito judiciously (mainly for mocking views)!
        // Opt for dependency injection and inheritance over Mockito
        mockForagePinEditText = mock(ForagePINEditText::class.java)
        val mockContext = mock(Context::class.java)
        mockBasisTheory = mock(BasisTheoryElements::class.java)
        mockBasisTheoryTextElement = mock(TextElement::class.java)
        `when`(mockForagePinEditText.getTextElement()).thenReturn(mockBasisTheoryTextElement)

        // ensure we don't make any live requests!
        mockBasisTheoryResponse(Result.success("success"))

        submitter = BasisTheoryPinSubmitter(
            foragePinEditText = mockForagePinEditText,
            logger = mockLogger,
            buildVaultProvider = { mockBasisTheory }
        )
    }

    private fun mockBasisTheoryResponse(response: BasisTheoryResponse) {
        mockApiProxy = mock(ProxyApi::class.java)
        `when`(mockBasisTheory.proxy).thenReturn(mockApiProxy)
        runBlocking {
            if (response.isSuccess) {
                `when`(mockApiProxy.post(anyOrNull(), anyOrNull())).thenReturn(response.getOrNull()!!)
            } else {
                `when`(mockApiProxy.post(anyOrNull(), anyOrNull())).thenThrow(response.exceptionOrNull()!!)
            }
        }
    }

    @Test
    fun `Basis Theory request receives expected params`() = runTest {
        StopgapGlobalState.forageConfig = ForageConfig("1234567", "sandbox_abcdefg123")

        val vaultProxyRequest = VaultProxyRequest.emptyRequest()
            .setHeader(ForageConstants.Headers.X_KEY, "12320ce0-1a3c-4c64-970c-51ed7db34548")
            .setHeader(ForageConstants.Headers.MERCHANT_ACCOUNT, "1234567")
            .setHeader(ForageConstants.Headers.IDEMPOTENCY_KEY, "abcdef123")
            .setHeader(ForageConstants.Headers.TRACE_ID, "65639248-03f2-498d-8aa8-9ebd1c60ee65")
            .setToken("45320ce0-1a3c-4c64-970c-51ed7db34548")
            .setPath("/api/payment_methods/defghij123/balance/")
            .setHeader(ForageConstants.Headers.BT_PROXY_KEY, StopgapGlobalState.envConfig.btProxyID)
            .setHeader(ForageConstants.Headers.CONTENT_TYPE, "application/json")

        runBlocking {
            submitter.submitProxyRequest(vaultProxyRequest)
        }

        val captor = argumentCaptor<ProxyRequest>()
        verify(mockApiProxy).post(captor.capture(), eq(null))
        val capturedRequest = captor.firstValue

        assertEquals(
            ProxyRequestObject(
                pin = mockBasisTheoryTextElement,
                card_number_token = "45320ce0-1a3c-4c64-970c-51ed7db34548"
            ),
            capturedRequest.body
        )
        assertEquals(
            hashMapOf(
                "X-KEY" to "12320ce0-1a3c-4c64-970c-51ed7db34548",
                "Merchant-Account" to "1234567",
                "IDEMPOTENCY-KEY" to "abcdef123",
                "x-datadog-trace-id" to "65639248-03f2-498d-8aa8-9ebd1c60ee65",
                "BT-PROXY-KEY" to "R1CNiogSdhnHeNq6ZFWrG1", // sandbox value of btProxyID
                "Content-Type" to "application/json"
            ),
            capturedRequest.headers
        )
        assertEquals("/api/payment_methods/defghij123/balance/", capturedRequest.path)
    }

    @Test
    fun `submitProxyRequest with valid input should return success`() = runTest {
        val responseStr = """{"content_id":"32489e7e-13d9-499c-b017-f68a0122da95","message_type":"0200","status":"sent_to_proxy","failed":false,"errors":[]}"""
        mockBasisTheoryResponse(Result.success(responseStr))

        val result = submitter.submitProxyRequest(VaultProxyRequest.emptyRequest())

        assertTrue(result is ForageApiResponse.Success)
        assertEquals("[basis_theory] Received successful response from basis_theory", mockLogger.infoLogs.last().getMessage())
        assertEquals(responseStr, (result as ForageApiResponse.Success).data)
    }

    @Test
    fun `Basis Theory returns a vault error`() = runTest {
        val basisTheoryErrorMessage = """
            Message: 
            HTTP response code: 400
            HTTP response body: {"proxy_error":{"errors":{"error":["Basis Theory Validation Error"]},"title":"One or more validation errors occurred.","status":400,"detail":"Bad Request"}}
            HTTP response headers: ...
        """.trimIndent()
        mockBasisTheoryResponse(Result.failure(RuntimeException(basisTheoryErrorMessage)))

        val result = submitter.submitProxyRequest(VaultProxyRequest.emptyRequest())

        assertTrue(result is ForageApiResponse.Failure)
        val firstError = (result as ForageApiResponse.Failure).errors[0]
        assertEquals("[basis_theory] Received error from basis_theory: java.lang.RuntimeException: $basisTheoryErrorMessage", mockLogger.errorLogs.last().getMessage())
        assertEquals("Unknown Server Error", firstError.message)
        assertEquals(500, firstError.httpStatusCode)
        assertEquals("unknown_server_error", firstError.code)
    }

    @Test
    fun `Basis Theory returns a ForageError`() = runTest {
        val responseStr = """
        Message:
        HTTP response code: 400
        HTTP response body: {"path": "/api/payments/abcdefg123/collect_pin/","errors": [{"code": "too_many_requests","message": "Request was throttled, please try again later."}]}
        HTTP response headers: {"some": "header"}
        """.trimIndent()

        mockBasisTheoryResponse(Result.failure(RuntimeException(responseStr)))

        val result = submitter.submitProxyRequest(VaultProxyRequest.emptyRequest())

        assertTrue(result is ForageApiResponse.Failure)
        val firstError = (result as ForageApiResponse.Failure).errors[0]
        assertEquals(
            """
        [basis_theory] Received ForageError from basis_theory: Code: too_many_requests
        Message: Request was throttled, please try again later.
        Status Code: 400
        Error Details (below):
        null
            """.trimIndent(),
            mockLogger.errorLogs.last().getMessage()
        )
        assertEquals("Request was throttled, please try again later.", firstError.message)
        assertEquals(400, firstError.httpStatusCode)
        assertEquals("too_many_requests", firstError.code)
    }

    @Test
    fun `Basis Theory responds with a malformed error`() = runTest {
        val responseStr = """
        Malformed error!
        """.trimIndent()

        mockBasisTheoryResponse(Result.failure(RuntimeException(responseStr)))

        val result = submitter.submitProxyRequest(VaultProxyRequest.emptyRequest())

        assertTrue(result is ForageApiResponse.Failure)
        val firstError = (result as ForageApiResponse.Failure).errors[0]
        assertEquals(
            "[basis_theory] Received malformed response from basis_theory: Failure(java.lang.RuntimeException: Malformed error!)",
            mockLogger.errorLogs.last().getMessage()
        )
        assertEquals("Unknown Server Error", firstError.message)
        assertEquals(500, firstError.httpStatusCode)
        assertEquals("unknown_server_error", firstError.code)
    }
}
