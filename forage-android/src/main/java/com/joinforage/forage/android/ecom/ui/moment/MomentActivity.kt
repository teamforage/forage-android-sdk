package com.joinforage.forage.android.ecom.ui.moment

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import com.joinforage.forage.android.core.services.forageapi.network.error.ForageError
import com.joinforage.forage.android.ecom.services.moment.MOMENT_BRIDGE_NAME
import com.joinforage.forage.android.ecom.services.moment.MomentResult
import com.joinforage.forage.android.ecom.services.moment.MomentSecurity
import com.joinforage.forage.android.ecom.services.moment.MomentSession
import com.joinforage.forage.android.ecom.services.moment.MomentSessions
import com.joinforage.forage.android.ecom.services.moment.parseMomentEvent

/**
 * Shows a moment's page full screen in a WebView owned by the SDK. Declares `configChanges`
 * in the manifest so rotation doesn't destroy the WebView mid-moment.
 */
internal class MomentActivity : ComponentActivity() {
    private var session: MomentSession? = null
    private var webView: WebView? = null
    private var teardown: (() -> Unit)? = null

    @SuppressLint("SetJavaScriptEnabled", "RequiresFeature")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // no session means the app process was killed and restarted. the createMoment call
        // is gone too, so there's nothing to show
        val session = MomentSessions.get(intent.getStringExtra(EXTRA_SESSION_ID))
        if (session == null) {
            finish()
            return
        }
        this.session = session
        val allowedOrigin = session.location.origin

        if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        val webView = WebView(this)
        this.webView = webView
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
        }
        // TODO(verify): the PIN field is a cross-origin vault iframe, which may need
        // third-party cookies. WebView blocks them by default
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        webView.webViewClient = MomentWebViewClient(allowedOrigin) {
            session.settle(MomentResult.Failed(renderProcessGoneError))
        }

        // the bridge object is only injected into frames from the moment's origin. we also
        // check each message, since only the top-level page may talk to us
        WebViewCompat.addWebMessageListener(
            webView,
            MOMENT_BRIDGE_NAME,
            setOf(allowedOrigin)
        ) { _: WebView, message: WebMessageCompat, sourceOrigin: Uri, isMainFrame: Boolean, replyProxy: JavaScriptReplyProxy ->
            if (!MomentSecurity.shouldAcceptMessage(isMainFrame, sourceOrigin.toString(), allowedOrigin)) {
                return@addWebMessageListener
            }
            val event = message.data?.let { parseMomentEvent(it) } ?: return@addWebMessageListener
            session.handle(event) { replyProxy.postMessage(it) }
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    session.back()
                }
            }
        )

        val teardown = {
            WebViewCompat.removeWebMessageListener(webView, MOMENT_BRIDGE_NAME)
            finish()
        }
        this.teardown = teardown
        session.teardown = teardown

        setContentView(edgeToEdgeContainer(webView))
        webView.loadUrl(session.location.url)
    }

    /**
     * Draws the moment behind transparent system bars and pads it out of their way, and out of the
     * keyboard's. Merchant apps targeting API 35+ are edge to edge on Android 15 whatever we do,
     * so we do it on every version to look the same everywhere.
     */
    private fun edgeToEdgeContainer(webView: WebView): FrameLayout {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        // dark status and navigation bar icons only exist from API 23 and 26, so the bars stay
        // opaque before then, or their light icons would be invisible on the page
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.statusBarColor = Color.TRANSPARENT
            insetsController.isAppearanceLightStatusBars = true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.navigationBarColor = Color.TRANSPARENT
            insetsController.isAppearanceLightNavigationBars = true
        }

        val container = FrameLayout(this)
        container.setBackgroundColor(Color.WHITE)
        container.addView(webView)
        ViewCompat.setOnApplyWindowInsetsListener(container) { view, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                    WindowInsetsCompat.Type.displayCutout() or
                    WindowInsetsCompat.Type.ime()
            )
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            WindowInsetsCompat.CONSUMED
        }
        return container
    }

    override fun onDestroy() {
        // finishing without settling (e.g. the system closed us) counts as a dismissal.
        // if we're only being recreated, the session stays open for the new activity
        if (isFinishing) {
            session?.settle(MomentResult.Dismissed)
        } else if (session?.teardown === teardown) {
            session?.teardown = null
        }
        webView?.destroy()
        webView = null
        super.onDestroy()
    }

    private class MomentWebViewClient(
        private val allowedOrigin: String,
        private val onRenderProcessGone: () -> Unit
    ) : WebViewClient() {
        @RequiresApi(Build.VERSION_CODES.N)
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
            MomentSecurity.shouldBlockNavigation(request.isForMainFrame, request.url.toString(), allowedOrigin)

        // before API 24 we can't tell the top-level page from iframes, so treat every
        // navigation as the top-level page. TODO(verify): the vault iframe loads on API 21-23
        @Deprecated("Deprecated in Java")
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
            MomentSecurity.shouldBlockNavigation(isForMainFrame = true, url = url, allowedOrigin = allowedOrigin)

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
            onRenderProcessGone()
            return true
        }
    }

    companion object {
        private const val EXTRA_SESSION_ID = "com.joinforage.forage.android.MOMENT_SESSION_ID"

        private val renderProcessGoneError = ForageError(
            500,
            "unknown_server_error",
            "The moment's WebView stopped unexpectedly"
        )

        fun intent(context: Context, sessionId: String): Intent =
            Intent(context, MomentActivity::class.java).putExtra(EXTRA_SESSION_ID, sessionId)
    }
}
