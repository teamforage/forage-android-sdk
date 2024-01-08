package com.joinforage.forage.android.pos

import com.joinforage.forage.android.CapturePaymentParams
import com.joinforage.forage.android.CheckBalanceParams
import com.joinforage.forage.android.DeferPaymentCaptureParams
import com.joinforage.forage.android.ForageSDKInterface
import com.joinforage.forage.android.TokenizeEBTCardParams
import com.joinforage.forage.android.mock.MockLogger
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.ui.ForagePANEditText
import com.joinforage.forage.android.ui.ForagePINEditText
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner

class MockForageSDK : ForageSDKInterface {
    override suspend fun tokenizeEBTCard(params: TokenizeEBTCardParams): ForageApiResponse<String> {
        return ForageApiResponse.Success("Success")
    }

    override suspend fun checkBalance(
        params: CheckBalanceParams
    ): ForageApiResponse<String> {
        return ForageApiResponse.Success("Success")
    }

    override suspend fun capturePayment(
        params: CapturePaymentParams
    ): ForageApiResponse<String> {
        return ForageApiResponse.Success("Success")
    }

    override suspend fun deferPaymentCapture(params: DeferPaymentCaptureParams): ForageApiResponse<String> {
        return ForageApiResponse.Success("Success")
    }
}

@RunWith(RobolectricTestRunner::class)
class ForageTerminalSDKTest {
    private lateinit var mockForagePanEditText: ForagePANEditText
    private lateinit var mockForagePinEditText: ForagePINEditText
    private lateinit var terminalSdk: ForageTerminalSDK
    private lateinit var mockLogger: MockLogger

    @Before
    fun setUp() {
        // Use Mockito judiciously (mainly for mocking views)!
        // Opt for dependency injection and inheritance over Mockito
        mockForagePanEditText = mock(ForagePANEditText::class.java)
        mockForagePinEditText = mock(ForagePINEditText::class.java)

        mockLogger = MockLogger()

        terminalSdk = ForageTerminalSDK(
            posTerminalId = "1234",
            forageSdk = MockForageSDK(),
            logger = mockLogger
        )
    }

    @Test
    fun `tokenize EBT card via UI-based PAN entry`() = runTest {
        val response = terminalSdk.tokenizeCard(
            foragePanEditText = mockForagePanEditText,
            reusable = true
        )

        val loggedMessage = mockLogger.infoLogs[0].getMessage()
        assertEquals(loggedMessage, "[POS] Tokenizing Payment Method via UI PAN entry")

        assertTrue(response is ForageApiResponse.Success)
        assertTrue((response as ForageApiResponse.Success).data == "Success")
    }

    @Test
    fun testCheckBalance() = runTest {
        val params = CheckBalanceParams(
            foragePinEditText = mockForagePinEditText,
            paymentMethodRef = "paymentMethod1234"
        )
        val response = terminalSdk.checkBalance(params)
        assertTrue(response is ForageApiResponse.Success)
        assertTrue((response as ForageApiResponse.Success).data == "TODO")
    }

    @Test
    fun testCapturePayment() = runTest {
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
        val params = DeferPaymentCaptureParams(
            foragePinEditText = mockForagePinEditText,
            paymentRef = "payment1234"
        )
        val response = terminalSdk.deferPaymentCapture(params)
        assertTrue(response is ForageApiResponse.Success)
        assertTrue((response as ForageApiResponse.Success).data == "Success")
    }
}
