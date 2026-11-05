package com.joinforage.forage.android.ecom.services.moment

import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.network.UnknownErrorApiResponse
import com.joinforage.forage.android.core.services.forageapi.network.error.ForageError
import org.json.JSONObject


internal object MomentProtocol {
    // This need to match what the internal JS SDK expects
    const val BRIDGE_NAME = "forageSdk"
    const val SDK_NAME = "forage-android-sdk"

    const val LOAD_DEADLINE_MS = 30_000L
    val LOAD_TIMEOUT_ERROR = ForageError(
        504,
        "timeout_error",
        "The moment didn't load in time. Try again later."
    )
}

/** The `initialize_iframe` command sent in reply to `input_hello` */
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
                .put("sdkName", MomentProtocol.SDK_NAME)
                .put("sdkVersion", sdkVersion)
        )
        .toString()

/** Events the page sends. Unknown types are ignored, so the page can add new ones. */
internal sealed class MomentEvent {
    object Hello : MomentEvent()
    object Ready : MomentEvent()
    object Success : MomentEvent()
    data class Failure(val error: ForageError) : MomentEvent()
    object Closed : MomentEvent()

    companion object {
        fun parse(message: String): MomentEvent? {
            val json = try {
                JSONObject(message)
            } catch (e: Exception) {
                return null
            }
            return when (json.optString("type")) {
                "input_hello" -> Hello
                "input_ready" -> Ready
                "submit_success" -> Success
                "submit_failure" -> Failure(forageError(json.opt("payload")))
                "moment_closed" -> Closed
                else -> null
            }
        }

        // a payload that isn't a ForageError becomes unknown_server_error
        private fun forageError(payload: Any?): ForageError {
            val json = payload as? JSONObject ?: return UnknownErrorApiResponse.error
            val code = json.opt("code") as? String
            val httpStatusCode = json.opt("httpStatusCode") as? Int
            val message = json.opt("message") as? String
            if (code == null || httpStatusCode == null || message == null) {
                return UnknownErrorApiResponse.error
            }
            return ForageError(httpStatusCode, code, message)
        }
    }
}
