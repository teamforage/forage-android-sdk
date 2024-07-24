package com.joinforage.forage.android.core.services.forageapi.network.error.payload

import com.joinforage.forage.android.core.services.forageapi.polling.ForageErrorDetails
import org.json.JSONArray
import org.json.JSONObject

/**
 * Converts error responses passed as lists to a [Failure] instance.
 * For example:
 *
 * {
 *   "path": "/api/session_token/",
 *   "errors": [
 *     {
 *       "code": "...",
 *       "message": "...",
 *       "source": {
 *         "resource": "...",
 *         "ref": ""
 *       }
 *     }
 *   ]
 * }
 */
internal class ErrorListResponsePayload(jsonErrorResponse: JSONObject) : ErrorPayload(jsonErrorResponse) {
    // dynamic properties so that they do not throw an error
    // during constructor calling
    private val errors: JSONArray
        get() = jsonErrorResponse.getJSONArray("errors")
    private val firstError: JSONObject
        get() = errors.getJSONObject(0)
    private val code: String
        get() = parseCode()

    override fun parseCode(): String = firstError.getString("code")
    override fun parseMessage(): String = firstError.getString("message")
    override fun parseDetails(): ForageErrorDetails? = ForageErrorDetails.from(code, firstError)
}
