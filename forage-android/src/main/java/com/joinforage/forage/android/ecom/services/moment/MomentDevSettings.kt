package com.joinforage.forage.android.ecom.services.moment

import androidx.annotation.RestrictTo

/**
 * For Forage's own development. Not part of the public API.
 *
 * Allows the sample app to render moments from a locally running page instead of deployed URL
 * derived from the session token's environment. Ignored when the session token is an environment
 * other than `dev` or `local`.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object MomentDevSettings {
    /**
     * An origin like `http://localhost:3000`. `null`, or a session token from any environment other
     * than `dev` or `local`, uses the token's environment.
     */
    @Volatile
    var baseUrlOverride: String? = null
}
