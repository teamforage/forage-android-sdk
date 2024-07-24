package com.joinforage.forage.android.core.services.forageapi.network.error.payload

import com.joinforage.forage.android.core.services.forageapi.network.error.ForageErrorDetails
import org.json.JSONObject

/**
 * Converts error responses with just a details field to a [Failure] instance.
 * For example:
 *
 * {
 *   "detail": "Authentication credentials were not provided."
 * }
 *
 * This error happens when you don't include the access_token
 * when trying to create a session token. Session tokens should
 * be created in the server so, in prod, we do not expect to
 * see these errors returned to the SDK
 *
 */
internal class RosettaBadRequestResponsePayload(
    jsonErrorResponse: JSONObject
) : ErrorPayload(jsonErrorResponse) {
    override fun parseCode(): String = "unknown_error"
    override fun parseMessage(): String = jsonErrorResponse.getString("detail")
    override fun parseDetails(): ForageErrorDetails? = null
}
