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
 * These are should never happen in production.
 */
internal class RosettaBadRequest(
    jsonErrorResponse: JSONObject
) : ErrorPayload(jsonErrorResponse) {
    override fun parseCode(): String = "unknown_error"
    override fun parseMessage(): String = jsonErrorResponse.getString("detail")
    override fun parseDetails(): ForageErrorDetails? = null
}
