package com.joinforage.forage.android.ecom.services.moment

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MomentSecurityTest {
    private val allowedOrigin = "https://custom-checkout.dev.joinforage.app"

    @Test
    fun `accepts messages from the moment's main frame`() {
        assertTrue(MomentSecurity.shouldAcceptMessage(isMainFrame = true, sourceOrigin = allowedOrigin, allowedOrigin = allowedOrigin))
        assertTrue(MomentSecurity.shouldAcceptMessage(isMainFrame = true, sourceOrigin = "http://10.0.2.2:3000", allowedOrigin = "http://10.0.2.2:3000"))
    }

    @Test
    fun `ignores messages from iframes, even on the moment's origin`() {
        // e.g. the PIN field's vault iframe
        assertFalse(MomentSecurity.shouldAcceptMessage(isMainFrame = false, sourceOrigin = "https://vault.example.com", allowedOrigin = allowedOrigin))
        assertFalse(MomentSecurity.shouldAcceptMessage(isMainFrame = false, sourceOrigin = allowedOrigin, allowedOrigin = allowedOrigin))
    }

    @Test
    fun `ignores messages from other origins`() {
        listOf(
            "https://evil.example.com",
            "http://custom-checkout.dev.joinforage.app",
            "https://custom-checkout.dev.joinforage.app:8443",
            "https://custom-checkout.dev.joinforage.app.evil.example.com",
            "not an origin"
        ).forEach { origin ->
            assertFalse(
                MomentSecurity.shouldAcceptMessage(isMainFrame = true, sourceOrigin = origin, allowedOrigin = allowedOrigin),
                origin
            )
        }
    }

    @Test
    fun `lets the main frame navigate within the moment's origin`() {
        assertFalse(MomentSecurity.shouldBlockNavigation(isForMainFrame = true, url = "$allowedOrigin/payment/collect-pin?paymentRef=other", allowedOrigin = allowedOrigin))
    }

    @Test
    fun `blocks the main frame from leaving the moment's origin`() {
        assertTrue(MomentSecurity.shouldBlockNavigation(isForMainFrame = true, url = "https://evil.example.com/", allowedOrigin = allowedOrigin))
        assertTrue(MomentSecurity.shouldBlockNavigation(isForMainFrame = true, url = "javascript:alert(1)", allowedOrigin = allowedOrigin))
    }

    @Test
    fun `lets iframes load from other origins`() {
        assertFalse(MomentSecurity.shouldBlockNavigation(isForMainFrame = false, url = "https://vault.example.com/pin", allowedOrigin = allowedOrigin))
    }
}
