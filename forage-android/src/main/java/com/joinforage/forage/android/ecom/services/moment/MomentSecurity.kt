package com.joinforage.forage.android.ecom.services.moment

/**
 * The checks that keep anything other than the moment's own page from using the bridge.
 *
 * The page doesn't check where messages come from, so these are what protect the moment.
 * `allowedOriginRules` already keeps the bridge object out of other origins' frames, and these
 * checks back it up.
 */
internal object MomentSecurity {
    /**
     * Whether to accept a message from the page. Only the moment's main frame on its own origin
     * can talk to us, not the iframes it embeds (like the PIN field's vault iframe).
     */
    fun shouldAcceptMessage(isMainFrame: Boolean, sourceOrigin: String, allowedOrigin: String): Boolean =
        isMainFrame && originOf(sourceOrigin) == allowedOrigin

    /**
     * Whether to block a navigation. The page itself can't navigate away from the moment's
     * origin, so nothing else can receive the bridge. Iframes in the page can load what they need.
     */
    fun shouldBlockNavigation(isForMainFrame: Boolean, url: String, allowedOrigin: String): Boolean =
        isForMainFrame && originOf(url) != allowedOrigin
}
