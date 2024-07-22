package com.joinforage.forage.android.core.services.forageapi.network.error.payload

import com.joinforage.forage.android.core.services.forageapi.network.error.ForageErrorDetails
import org.json.JSONObject

/**
 * Converts error responses passed as single errors to a [Failure] instance.
 * For example:
 *
 * {
 *   "ref": "e1fff94f29",
 *   "balance": null,
 *   "content_id": "c1898593-fa3d-4a1c-b16b-10ecc38b3619",
 *   "error": {
 *     "message": "Invalid card number - Re-enter Transaction",
 *     "forage_code": "ebt_error_14",
 *     "status_code": 400
 *   }
 * }
 */
internal class SingleErrorResponse(
    jsonErrorResponse: JSONObject
) : ErrorPayload(jsonErrorResponse) {
    // dynamic properties so that they do not throw an error
    // during constructor calling
    private val error: JSONObject
        get() = jsonErrorResponse.getJSONObject("error")
    private val code: String
        get() = parseCode()

    override fun parseCode(): String = error.getString("forage_code")
    override fun parseMessage(): String = error.getString("message")
    override fun parseDetails(): ForageErrorDetails? = ForageErrorDetails.from(code, error)
}
