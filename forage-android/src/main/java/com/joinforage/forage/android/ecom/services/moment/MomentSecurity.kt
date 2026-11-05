package com.joinforage.forage.android.ecom.services.moment

/**
 * Verifies the messages being received are from the known moment URL
 */
internal object MomentSecurity {
    /**
     * Whether to accept a message from the page. Check isMainFrame to ensure we only get moment
     * events and don't get events from other things embedded within the moment (like input-iframes).
     */
    fun shouldAcceptMessage(isMainFrame: Boolean, sourceOrigin: String, allowedOrigin: String): Boolean =
        isMainFrame && originOf(sourceOrigin) == allowedOrigin

    /**
     * Whether to allow a navigation. The page itself can't navigate away from the moment's
     * origin, so nothing else can receive the bridge. Iframes in the page can load what they need.
     */
    fun shouldAllowNavigation(isForMainFrame: Boolean, url: String, allowedOrigin: String): Boolean =
        !isForMainFrame || originOf(url) == allowedOrigin
}
