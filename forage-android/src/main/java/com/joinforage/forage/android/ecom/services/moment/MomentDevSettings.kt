package com.joinforage.forage.android.ecom.services.moment

import androidx.annotation.RestrictTo

/**
 * For Forage's own development. Not part of the public API.
 *
 * Lets the sample app point moments at a locally running page (e.g. custom-checkout at
 * `http://10.0.2.2:3000` from the emulator) instead of the URL for the session token's
 * environment. Only `dev` and `local` session tokens use it, so it's ignored everywhere else.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object MomentDevSettings {
    /**
     * An origin like `http://10.0.2.2:3000`. `null`, or a session token from any environment other
     * than `dev` or `local`, uses the token's environment.
     */
    @Volatile
    var baseUrlOverride: String? = null
}
