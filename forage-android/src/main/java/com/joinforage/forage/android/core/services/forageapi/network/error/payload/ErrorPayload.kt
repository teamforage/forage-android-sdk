package com.joinforage.forage.android.core.services.forageapi.network.error.payload

import com.joinforage.forage.android.core.services.forageapi.network.error.ForageErrorDetails
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
                SingleErrorResponse(jsonErrorResponse).isMatch() -> {
                    return SingleErrorResponse(jsonErrorResponse)
                }
                ErrorListResponse(jsonErrorResponse).isMatch() -> {
                    return ErrorListResponse(jsonErrorResponse)
                }
                RosettaBadRequest(jsonErrorResponse).isMatch() -> {
                    return RosettaBadRequest(jsonErrorResponse)
                }
                RosettaErrorResponse(jsonErrorResponse).isMatch() -> {
                    return RosettaErrorResponse(jsonErrorResponse)
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
