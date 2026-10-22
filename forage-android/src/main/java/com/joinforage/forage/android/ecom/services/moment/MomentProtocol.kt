package com.joinforage.forage.android.ecom.services.moment

import com.joinforage.forage.android.core.services.EnvConfig
import com.joinforage.forage.android.core.services.EnvOption
import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.network.error.ForageError
import org.json.JSONObject
import java.net.URI
import java.net.URLEncoder

// The page <-> SDK protocol from fe-monorepo's apps/sdk/docs/moments-protocol.md.
// Names must match the page bridge in sdk-internal (hosted-page/platform-messaging.ts).

/** The object the page posts to, injected with `WebViewCompat.addWebMessageListener` */
internal const val MOMENT_BRIDGE_NAME = "forageSdk"

internal const val MOMENT_SDK_NAME = "forage-android-sdk"

internal const val MOMENT_LOAD_DEADLINE_MS = 30_000L

// the same code the SDK uses for other timeouts
internal val momentLoadTimeoutError = ForageError(
    504,
    "timeout_error",
    "The moment didn't load in time. Try again later."
)

private const val SERVICE = "custom-checkout"

/** Where the moment's page is served for the session token's environment */
internal fun momentOrigin(envConfig: EnvConfig): String = when (envConfig.FLAVOR) {
    EnvOption.PROD -> "https://$SERVICE.joinforage.app"
    EnvOption.SANDBOX, EnvOption.STAGING, EnvOption.CERT ->
        "https://$SERVICE.${envConfig.FLAVOR.value}.joinforage.app"
    EnvOption.LOCAL -> "http://$SERVICE.joinforage.localhost"
    EnvOption.DEV, EnvOption.MOCK -> "https://$SERVICE.dev.joinforage.app"
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
    // only Forage's internal environments can use the override, so it does nothing for
    // sandbox and prod tokens however a merchant reaches it
    val canOverride = forageConfig.envConfig.FLAVOR in setOf(EnvOption.DEV, EnvOption.LOCAL)
    val origin = baseUrlOverride?.takeIf { canOverride }?.let { originOf(it) }
        ?: momentOrigin(forageConfig.envConfig)
    val query = moment.params.entries.joinToString("&") { (key, value) ->
        "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
    }
    return MomentLocation(origin, "$origin${moment.path}?$query")
}

/** The `initialize_iframe` command, sent in reply to every `input_hello` */
internal fun momentInitMessage(forageConfig: ForageConfig, sdkVersion: String): String =
    JSONObject()
        .put("name", "initialize_iframe")
        .put(
            "payload",
            JSONObject()
                .put("merchantId", forageConfig.merchantId)
                .put("sessionToken", forageConfig.sessionToken)
                .put("env", forageConfig.envConfig.FLAVOR.value)
                .put("usecase", "moment")
                .put("sdkName", MOMENT_SDK_NAME)
                .put("sdkVersion", sdkVersion)
        )
        .toString()

/** Events the page sends. Unknown types are ignored, so the page can add new ones. */
internal sealed class MomentEvent {
    object Hello : MomentEvent()
    object Ready : MomentEvent()
    object Succeeded : MomentEvent()
    data class Failed(val error: ForageError) : MomentEvent()
    object Closed : MomentEvent()
}

internal fun parseMomentEvent(message: String): MomentEvent? {
    val json = try {
        JSONObject(message)
    } catch (e: Exception) {
        return null
    }
    return when (json.optString("type")) {
        "input_hello" -> MomentEvent.Hello
        "input_ready" -> MomentEvent.Ready
        "submit_success" -> MomentEvent.Succeeded
        "submit_failure" -> MomentEvent.Failed(toForageError(json.optJSONObject("payload")))
        "moment_closed" -> MomentEvent.Closed
        else -> null
    }
}

// a payload that isn't a ForageError becomes unknown_server_error
private fun toForageError(payload: JSONObject?): ForageError {
    val code = payload?.optString("code").orEmpty()
    val status = payload?.optInt("httpStatusCode", 0) ?: 0
    if (code.isEmpty() || status == 0) {
        return ForageError(500, "unknown_server_error", "Unknown Server Error")
    }
    return ForageError(status, code, payload?.optString("message").orEmpty())
}
