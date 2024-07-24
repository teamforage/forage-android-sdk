package com.joinforage.forage.android.core.services.forageapi.network.error.payload

import com.joinforage.forage.android.core.services.forageapi.polling.ForageErrorDetails
import org.json.JSONException
import org.json.JSONObject

class UnexpectedResponseError(jsonStringResponse: String) :
    Exception(jsonStringResponse)

internal abstract class ErrorPayload(
    val jsonErrorResponse: JSONObject
) {

    abstract fun parseCode(): String
    abstract fun parseMessage(): String
    abstract fun parseDetails(): ForageErrorDetails?

    // If we're able to parse the code, message, and details
    // from the JSON object, then it's a match
    fun isMatch(): Boolean {
        try {
            parseCode()
            parseMessage()
            parseDetails()
            return true
        } catch (e: JSONException) {
            return false
        }
    }

    companion object {
        fun parseJson(jsonErrorResponse: JSONObject): ErrorPayload {
            when {
                SingleErrorResponsePayload(jsonErrorResponse).isMatch() -> {
                    return SingleErrorResponsePayload(jsonErrorResponse)
                }
                ErrorListResponsePayload(jsonErrorResponse).isMatch() -> {
                    return ErrorListResponsePayload(jsonErrorResponse)
                }
                RosettaBadRequestResponsePayload(jsonErrorResponse).isMatch() -> {
                    return RosettaBadRequestResponsePayload(jsonErrorResponse)
                }
                RosettaErrorResponsePayload(jsonErrorResponse).isMatch() -> {
                    return RosettaErrorResponsePayload(jsonErrorResponse)
                }
                else -> {
                    throw UnexpectedResponseError(jsonErrorResponse.toString())
                }
            }
        }

        fun parseJsonString(jsonStringErrorResponse: String): ErrorPayload =
            parseJson(JSONObject(jsonStringErrorResponse))
    }
}
