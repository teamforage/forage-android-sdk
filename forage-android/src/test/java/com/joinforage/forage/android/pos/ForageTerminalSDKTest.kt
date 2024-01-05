package com.joinforage.forage.android.pos

import android.content.Context
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import com.joinforage.forage.android.CapturePaymentParams
import com.joinforage.forage.android.CheckBalanceParams
import com.joinforage.forage.android.DeferPaymentCaptureParams
import com.joinforage.forage.android.ForageSDKInterface
import com.joinforage.forage.android.TokenizeEBTCardParams
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.ui.ForagePANEditText
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
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
    private lateinit var context: Context
    private lateinit var terminalSdk: ForageTerminalSDK
    private lateinit var mockForagePanEditText: ForagePANEditText


    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mockForagePanEditText = ForagePANEditText(context)
        launchFragmentInContainer<CatalogFragment>(themeResId = R.style.Theme_ForageAndroid)

        terminalSdk = ForageTerminalSDK(posTerminalId = "1234", forageSdk = MockForageSDK())
    }

    @Test
    fun testInitialization() {
        assertNotNull(terminalSdk)
        assertTrue( "Does not throw!", true)
    }

    @Test(expected = NotImplementedError::class)
    fun testCallsTokenizeCardNotApplicableMethod() {
        runBlocking {
            terminalSdk.tokenizeEBTCard(
                params = TokenizeEBTCardParams(
                    foragePanEditText = mockForagePanEditText,
                    customerId = "1234",
                    reusable = true
                )
            )
        }
    }
}