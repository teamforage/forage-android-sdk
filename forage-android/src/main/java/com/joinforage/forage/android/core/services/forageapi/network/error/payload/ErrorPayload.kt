package com.joinforage.forage.android.core.services.forageapi.network.error.payload

import com.joinforage.forage.android.core.services.forageapi.polling.ForageErrorDetails
import org.json.JSONException
import org.json.JSONObject

internal abstract class ErrorPayload(
    val jsonErrorResponse: JSONObject
) {
    class UnknownForageFailureResponse(val rawResponse: String) :
        Exception("Unknown Forage Failure Response: $rawResponse")

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
                DeferredRefundErrorResponsePayload(jsonErrorResponse).isMatch() -> {
                    return DeferredRefundErrorResponsePayload(jsonErrorResponse)
                }
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
                    throw UnknownForageFailureResponse(jsonErrorResponse.toString())
                }
            }
        }

        fun parseJsonString(jsonStringErrorResponse: String): ErrorPayload =
            parseJson(JSONObject(jsonStringErrorResponse))
    }
}
