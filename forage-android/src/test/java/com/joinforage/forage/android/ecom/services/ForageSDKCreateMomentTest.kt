package com.joinforage.forage.android.ecom.services

import androidx.activity.ComponentActivity
import androidx.webkit.WebViewFeature
import com.joinforage.forage.android.core.logger.InMemoryLogger
import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.telemetry.LogAttributes
import com.joinforage.forage.android.ecom.services.moment.MOMENT_TEST_SESSION_TOKEN
import com.joinforage.forage.android.ecom.services.moment.Moment
import com.joinforage.forage.android.ecom.services.moment.MomentResult
import com.joinforage.forage.android.ecom.services.moment.MomentSessions
import com.joinforage.forage.android.ecom.ui.moment.MomentActivity
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ForageSDKCreateMomentTest {
    private val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
    private val forageConfig = ForageConfig(merchantId = "mid123", sessionToken = MOMENT_TEST_SESSION_TOKEN)

    // the real logger starts the process-wide Datadog SDK, which then breaks later tests
    private val logger = InMemoryLogger(LogAttributes(forageConfig, traceId = "trace"))

    // Robolectric has no real WebView, so we say whether it supports moments
    private lateinit var webViewFeature: MockedStatic<WebViewFeature>

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        webViewFeature = mockStatic(WebViewFeature::class.java)
    }

    @After
    fun tearDown() {
        webViewFeature.close()
        Dispatchers.resetMain()
    }

    private fun params() =
        CreateMomentParams(forageConfig, Moment.PaymentCollectPin(paymentRef = "abc123"))

    private fun webViewSupportsMoments(supported: Boolean) {
        webViewFeature.`when`<Boolean> {
            WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)
        }.thenReturn(supported)
    }

    @Test
    fun `opens the moment in MomentActivity and returns how it ended`() = runTest {
        webViewSupportsMoments(true)

        val call = async(start = CoroutineStart.UNDISPATCHED) { ForageSDK().createMoment(activity, params(), logger) }

        val started = assertNotNull(shadowOf(activity).nextStartedActivity)
        assertEquals(MomentActivity::class.java.name, started.component?.className)
        val sessionId = started.extras?.keySet()?.single()?.let { started.getStringExtra(it) }
        assertNotNull(MomentSessions.get(sessionId)).settle(MomentResult.Dismissed)
        assertEquals(MomentResult.Dismissed, call.await())
    }

    @Test
    fun `fails without opening when the WebView doesn't support moments`() = runTest {
        webViewSupportsMoments(false)

        val result = ForageSDK().createMoment(activity, params(), logger)

        assertEquals("unknown_server_error", (result as MomentResult.Failed).error.code)
        assertNull(shadowOf(activity).nextStartedActivity)
    }
}
