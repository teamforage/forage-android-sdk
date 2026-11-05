package com.joinforage.forage.android.ecom.services.moment

import com.joinforage.forage.android.core.services.EnvConfig
import com.joinforage.forage.android.core.services.EnvOption
import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.network.error.ForageError
import java.net.URI
import java.net.URLEncoder

/**
 * A Forage-hosted page that [createMoment][com.joinforage.forage.android.ecom.services.ForageSDK.createMoment]
 * opens for the customer to complete a task.
 */
abstract class Moment internal constructor() {
    // where the moment's page lives and the params it goes in its URL
    internal abstract val name: String
    internal abstract val path: String
    internal abstract val params: Map<String, String>

    /**
     * Collects the customer's EBT Card PIN for a previously created
     * [`Payment`](https://docs.joinforage.app/reference/payments).
     *
     * @property paymentRef The `ref` of the `Payment`, returned by the
     * [Create a `Payment`](https://docs.joinforage.app/reference/create-a-payment) endpoint.
     */
    data class PaymentCollectPin(val paymentRef: String) : Moment() {
        override val name get() = "payment_collect_pin"
        override val path get() = "/payment/collect-pin"
        override val params get() = mapOf("paymentRef" to paymentRef)
    }
}

/**
 * The outcome of a [Moment].
 */
sealed class MomentResult {
    /**
     * The customer completed the moment.
     */
    object Completed : MomentResult() {
        override fun toString() = "Completed"
    }

    /**
     * The customer closed the moment without completing it.
     */
    object Dismissed : MomentResult() {
        override fun toString() = "Dismissed"
    }

    /**
     * The moment failed, or couldn't be opened.
     *
     * @property error A [ForageError] that describes what went wrong.
     */
    data class Failed(val error: ForageError) : MomentResult()
}

internal val EnvConfig.customCheckoutOrigin: String
    get() = when (FLAVOR) {
        EnvOption.PROD -> "https://custom-checkout.joinforage.app"
        EnvOption.SANDBOX, EnvOption.STAGING, EnvOption.CERT ->
            "https://custom-checkout.${FLAVOR.value}.joinforage.app"
        EnvOption.LOCAL -> "http://custom-checkout.joinforage.localhost"
        EnvOption.DEV, EnvOption.MOCK -> "https://custom-checkout.dev.joinforage.app"
    }

/** Reduces a URL to `scheme://host[:port]`, the form `allowedOriginRules` and `sourceOrigin` use */
internal fun originOf(url: String): String? = try {
    val uri = URI(url)
    val scheme = uri.scheme?.lowercase()
    val host = uri.host?.lowercase()
    if (scheme == null || host == null) {
        null
    } else if (uri.port == -1) {
        "$scheme://$host"
    } else {
        "$scheme://$host:${uri.port}"
    }
} catch (e: Exception) {
    null
}

internal data class MomentLocation(val origin: String, val url: String)

internal fun momentLocation(
    forageConfig: ForageConfig,
    moment: Moment,
    baseUrlOverride: String? = MomentDevSettings.baseUrlOverride
): MomentLocation {
    val canOverride = forageConfig.envConfig.FLAVOR in setOf(EnvOption.DEV, EnvOption.LOCAL)
    val origin = baseUrlOverride?.takeIf { canOverride }?.let { originOf(it) }
        ?: forageConfig.envConfig.customCheckoutOrigin
    val query = moment.params.entries.sortedBy { it.key }.joinToString("&") { (key, value) ->
        "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
    }
    return MomentLocation(origin, "$origin${moment.path}?$query")
}
