package com.joinforage.forage.android.ecom.ui.moment

import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import com.joinforage.forage.android.core.logger.InMemoryLogger
import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.telemetry.LogAttributes
import com.joinforage.forage.android.ecom.services.moment.MOMENT_BRIDGE_NAME
import com.joinforage.forage.android.ecom.services.moment.MOMENT_TEST_SESSION_TOKEN
import com.joinforage.forage.android.ecom.services.moment.MomentLocation
import com.joinforage.forage.android.ecom.services.moment.MomentResult
import com.joinforage.forage.android.ecom.services.moment.MomentSession
import com.joinforage.forage.android.ecom.services.moment.MomentSessions
import com.joinforage.forage.android.ecom.services.moment.MomentTelemetry
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class MomentActivityTest {
    private val forageConfig = ForageConfig(merchantId = "mid123", sessionToken = MOMENT_TEST_SESSION_TOKEN)
    private val logger = InMemoryLogger(LogAttributes(forageConfig, traceId = "trace"))
    private val results = mutableListOf<MomentResult>()
    private val replyProxy = mock<JavaScriptReplyProxy>()

    // Robolectric's WebView can't run androidx.webkit, so we stand in for it and capture the
    // listener the activity registers
    private lateinit var webViewCompat: MockedStatic<WebViewCompat>
    private lateinit var session: MomentSession

    @Before
    fun setUp() {
        webViewCompat = mockStatic(WebViewCompat::class.java)
        session = MomentSession(
            location = MomentLocation(ORIGIN, "$ORIGIN/payment/collect-pin?paymentRef=abc"),
            initMessage = INIT_MESSAGE,
            telemetry = MomentTelemetry(logger, "payment_collect_pin")
        ) { results.add(it) }
        MomentSessions.open(session)
    }

    @After
    fun tearDown() {
        webViewCompat.close()
        // MomentSessions is a process-wide singleton, so don't leak an open session into other tests
        MomentSessions.remove(session)
    }

    private fun launch(sessionId: String = session.id): ActivityController<MomentActivity> =
        Robolectric.buildActivity(
            MomentActivity::class.java,
            MomentActivity.intent(RuntimeEnvironment.getApplication(), sessionId)
        ).setup()

    private fun MomentActivity.container() =
        findViewById<ViewGroup>(android.R.id.content).getChildAt(0) as FrameLayout

    private fun MomentActivity.webView() = container().getChildAt(0) as WebView

    /** The listener the activity registered for the page's messages, for this origin only. */
    private fun listener(): WebViewCompat.WebMessageListener {
        val captor = argumentCaptor<WebViewCompat.WebMessageListener>()
        webViewCompat.verify {
            WebViewCompat.addWebMessageListener(any(), eq(MOMENT_BRIDGE_NAME), eq(setOf(ORIGIN)), captor.capture())
        }
        return captor.lastValue
    }

    private fun MomentActivity.receive(data: String?, origin: String = ORIGIN, isMainFrame: Boolean = true) =
        listener().onPostMessage(webView(), WebMessageCompat(data), Uri.parse(origin), isMainFrame, replyProxy)

    @Test
    fun `finishes right away when its moment is gone`() {
        // e.g. the process was killed and restarted, so the createMoment call is gone too
        val activity = launch(sessionId = "not-open").get()

        assertTrue(activity.isFinishing)
        webViewCompat.verify({ WebViewCompat.addWebMessageListener(any(), any(), any(), any()) }, never())
    }

    @Test
    fun `loads the moment's page in a locked down WebView`() {
        val webView = launch().get().webView()

        assertEquals(session.location.url, shadowOf(webView).lastLoadedUrl)
        assertTrue(webView.settings.javaScriptEnabled)
        assertFalse(webView.settings.allowFileAccess)
        assertFalse(webView.settings.allowContentAccess)
        listener()
    }

    @Test
    fun `replies to the page's hello with the init message`() {
        launch().get().receive("""{"type":"input_hello"}""")

        verify(replyProxy).postMessage(INIT_MESSAGE)
    }

    @Test
    fun `ignores messages from iframes, other origins, and ones it can't parse`() {
        val activity = launch().get()

        activity.receive("""{"type":"input_hello"}""", isMainFrame = false)
        activity.receive("""{"type":"input_hello"}""", origin = "https://evil.example.com")
        activity.receive(null)
        activity.receive("not json")

        verify(replyProxy, never()).postMessage(any<String>())
        assertTrue(results.isEmpty())
    }

    @Test
    fun `closes and stops listening once the page completes`() {
        val activity = launch().get()

        activity.receive("""{"type":"submit_success"}""")

        assertEquals(listOf<MomentResult>(MomentResult.Completed), results)
        assertTrue(activity.isFinishing)
        webViewCompat.verify { WebViewCompat.removeWebMessageListener(any(), eq(MOMENT_BRIDGE_NAME)) }
    }

    @Test
    fun `Back dismisses the moment`() {
        val activity = launch().get()

        activity.onBackPressedDispatcher.onBackPressed()

        assertEquals(listOf<MomentResult>(MomentResult.Dismissed), results)
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `finishing without a result counts as dismissed`() {
        val controller = launch()

        controller.get().finish()
        controller.pause().stop().destroy()

        assertEquals(listOf<MomentResult>(MomentResult.Dismissed), results)
    }

    @Test
    fun `recreating the activity keeps the moment open`() {
        val controller = launch()

        controller.recreate()

        assertTrue(results.isEmpty())
        assertNotNull(session.teardown)
        // the new activity takes over the moment
        controller.get().onBackPressedDispatcher.onBackPressed()
        assertEquals(listOf<MomentResult>(MomentResult.Dismissed), results)
        assertTrue(controller.get().isFinishing)
    }

    @Test
    fun `only lets the top-level page navigate on the moment's origin`() {
        val client = launch().get().webView().webViewClient
        fun request(url: String, isForMainFrame: Boolean) = mock<WebResourceRequest> {
            on { this.url } doReturn Uri.parse(url)
            on { this.isForMainFrame } doReturn isForMainFrame
        }
        val webView = mock<WebView>()

        assertFalse(client.shouldOverrideUrlLoading(webView, request("$ORIGIN/other", isForMainFrame = true)))
        assertTrue(client.shouldOverrideUrlLoading(webView, request("https://evil.example.com", isForMainFrame = true)))
        assertFalse(client.shouldOverrideUrlLoading(webView, request("https://vault.example.com", isForMainFrame = false)))
        @Suppress("DEPRECATION")
        assertTrue(client.shouldOverrideUrlLoading(webView, "https://evil.example.com"))
    }

    @Test
    fun `fails the moment if the WebView's renderer stops`() {
        val activity = launch().get()

        val handled = activity.webView().webViewClient.onRenderProcessGone(activity.webView(), mock())

        assertTrue(handled)
        assertEquals("unknown_server_error", (results.single() as MomentResult.Failed).error.code)
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `pads the page out of the way of the system bars and keyboard`() {
        val container = launch().get().container()
        val insets = WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(1, 2, 3, 4))
            .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, 300))
            .build()

        ViewCompat.dispatchApplyWindowInsets(container, insets)

        assertEquals(listOf(1, 2, 3, 300), listOf(container.paddingLeft, container.paddingTop, container.paddingRight, container.paddingBottom))
    }

    private companion object {
        const val ORIGIN = "https://custom-checkout.dev.joinforage.app"
        const val INIT_MESSAGE = """{"name":"initialize_iframe"}"""
    }
}
