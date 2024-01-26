package com.joinforage.forage.android.vault

import android.content.Context
import com.joinforage.forage.android.mock.MockLogger
import com.joinforage.forage.android.network.ForageConstants
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.ui.ForagePINEditText
import com.verygoodsecurity.vgscollect.core.HTTPMethod
import com.verygoodsecurity.vgscollect.core.VGSCollect
import com.verygoodsecurity.vgscollect.core.VgsCollectResponseListener
import com.verygoodsecurity.vgscollect.core.model.network.VGSRequest
import com.verygoodsecurity.vgscollect.core.model.network.VGSResponse
import com.verygoodsecurity.vgscollect.widget.VGSEditText
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import me.jorgecastillo.hiroaki.internal.MockServerSuite
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.anyOrNull

class VgsPinSubmitterTest() : MockServerSuite() {
    private lateinit var mockLogger: MockLogger
    private lateinit var mockForagePinEditText: ForagePINEditText
    private lateinit var vgsPinSubmitter: VgsPinSubmitter
    private lateinit var mockVgsCollect: VGSCollect

    @Before
    fun setUp() {
        super.setup()

        mockLogger = MockLogger()

        // Use Mockito judiciously (mainly for mocking views)!
        // Opt for dependency injection and inheritance over Mockito
        mockForagePinEditText = mock(ForagePINEditText::class.java)
        val mockContext = mock(Context::class.java)
        mockVgsCollect = mock(VGSCollect::class.java)
        `when`(mockForagePinEditText.getTextInputEditText()).thenReturn(mock(VGSEditText::class.java))
        `when`(mockVgsCollect.asyncSubmit(anyOrNull())).thenAnswer {}

        vgsPinSubmitter = VgsPinSubmitter(
            context = mockContext,
            foragePinEditText = mockForagePinEditText,
            logger = mockLogger,
            buildVaultProvider = { _ -> mockVgsCollect }
        )
    }

    private fun mockVgsCollectResponse(response: VGSResponse) {
        `when`(mockVgsCollect.addOnResponseListeners(any())).thenAnswer {
            val listener = it.arguments[0] as VgsCollectResponseListener
            listener.onResponse(response)
            null
        }
    }

    @Test
    fun `VGS request receives expected params`() = runTest {
        mockVgsCollectResponse(VGSResponse.SuccessResponse(successCode = 200, rawResponse = ""))

        val vaultProxyRequest = VaultProxyRequest.emptyRequest()
            .setHeader(ForageConstants.Headers.X_KEY, "tok_sandbox_sYiPe9Q249qQ5wQyUPP5f7")
            .setHeader(ForageConstants.Headers.MERCHANT_ACCOUNT, "1234567")
            .setHeader(ForageConstants.Headers.IDEMPOTENCY_KEY, "abcdef123")
            .setHeader(ForageConstants.Headers.TRACE_ID, "65639248-03f2-498d-8aa8-9ebd1c60ee65")
            .setToken("tok_sandbox_sYiPe9Q249qQ5wQyUPP5f7")
            .setPath("/api/payments/abcdefg123/capture/")

        vgsPinSubmitter.submitProxyRequest(vaultProxyRequest)

        verify(mockVgsCollect).asyncSubmit(
            VGSRequest.VGSRequestBuilder()
                .setMethod(HTTPMethod.POST)
                .setPath("/api/payments/abcdefg123/capture/")
                .setCustomHeader(
                    mapOf(
                        ForageConstants.Headers.X_KEY to "tok_sandbox_sYiPe9Q249qQ5wQyUPP5f7",
                        ForageConstants.Headers.MERCHANT_ACCOUNT to "1234567",
                        ForageConstants.Headers.IDEMPOTENCY_KEY to "abcdef123",
                        ForageConstants.Headers.TRACE_ID to "65639248-03f2-498d-8aa8-9ebd1c60ee65"
                    )
                )
                .setCustomData(
                    hashMapOf(
                        "card_number_token" to "tok_sandbox_sYiPe9Q249qQ5wQyUPP5f7"
                    )
                )
                .build()
        )
    }

    @Test
    fun `submitProxyRequest with valid input should return success`() = runTest {
        val responseStr = """{"content_id":"32489e7e-13d9-499c-b017-f68a0122da95","message_type":"0200","status":"sent_to_proxy","failed":false,"errors":[]}"""
        mockVgsCollectResponse(VGSResponse.SuccessResponse(successCode = 200, rawResponse = responseStr))

        val result = vgsPinSubmitter.submitProxyRequest(VaultProxyRequest.emptyRequest())

        assertTrue(result is ForageApiResponse.Success)
        assertEquals("[vgs] Received successful response from vgs", mockLogger.infoLogs.last().getMessage())
        assertEquals(responseStr, (result as ForageApiResponse.Success).data)
    }

    @Test
    fun `VGS returns a vault error`() = runTest {
        mockVgsCollectResponse(VGSResponse.ErrorResponse(errorCode = 403, rawResponse = "VGS connection error"))

        val result = vgsPinSubmitter.submitProxyRequest(VaultProxyRequest.emptyRequest())

        assertTrue(result is ForageApiResponse.Failure)
        val firstError = (result as ForageApiResponse.Failure).errors[0]
        assertEquals("[vgs] Received error from vgs: VGS connection error", mockLogger.errorLogs.last().getMessage())
        assertEquals("Unknown Server Error", firstError.message)
        assertEquals(500, firstError.httpStatusCode)
        assertEquals("unknown_server_error", firstError.code)
    }

    @Test
    fun `VGS returns a ForageError`() = runTest {
        val responseStr = """
        {
            "path": "/api/payments/abcdefg123/capture/",
            "errors": [
                {
                    "code": "cannot_capture_payment",
                    "message": "Payment with ref abcdefg123 is either processing, succeeded, or canceled and therefore cannot be captured."
                }
            ]
        }
        """.trimIndent()
        mockVgsCollectResponse(VGSResponse.ErrorResponse(errorCode = 400, rawResponse = responseStr))

        val result = vgsPinSubmitter.submitProxyRequest(VaultProxyRequest.emptyRequest())

        assertTrue(result is ForageApiResponse.Failure)
        val firstError = (result as ForageApiResponse.Failure).errors[0]
        assertEquals(
            """
        [vgs] Received ForageError from vgs: Code: cannot_capture_payment
        Message: Payment with ref abcdefg123 is either processing, succeeded, or canceled and therefore cannot be captured.
        Status Code: 400
        Error Details (below):
        null
            """.trimIndent(),
            mockLogger.errorLogs.last().getMessage()
        )
        assertEquals("Payment with ref abcdefg123 is either processing, succeeded, or canceled and therefore cannot be captured.", firstError.message)
        assertEquals(400, firstError.httpStatusCode)
        assertEquals("cannot_capture_payment", firstError.code)
    }
}
