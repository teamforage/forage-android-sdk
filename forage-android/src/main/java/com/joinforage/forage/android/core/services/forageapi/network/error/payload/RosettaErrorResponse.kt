package com.joinforage.forage.android.core.services.forageapi.network.error.payload

import com.joinforage.forage.android.core.services.forageapi.network.error.ForageErrorDetails
import org.json.JSONObject

/**
 * Converts error responses with just a details field to a [Failure] instance.
 * For example:
 *
 * {
 *   "code": 401,
 *   "description": "authorization header malformed",
 *   "name": "auth_header_malformed"
 * }
 *
 * This is error happens when you don't pass Bearer: session_token
 * as the auth header when accessing Rosetta. The SDK ought to be
 * passing this header correctly so we would not expect to see
 * this errors returned to the SDK in prod
 */
internal class RosettaErrorResponse(
    jsonErrorResponse: JSONObject
) : ErrorPayload(jsonErrorResponse) {
    override fun parseCode(): String = jsonErrorResponse.getString("name")
    override fun parseMessage(): String = jsonErrorResponse.getString("description")
    override fun parseDetails(): ForageErrorDetails? = null
}
