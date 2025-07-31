package com.joinforage.forage.android.core.services.forageapi.network.error

import com.joinforage.forage.android.core.services.forageapi.network.error.payload.ErrorPayload
import org.json.JSONObject

internal abstract class ForageErrorResponseParser {
    internal fun toForageError(httpStatusCode: Int, jsonString: String): ForageError {
        return try {
            toForageError(httpStatusCode, parseJson(jsonString))
        } catch (e: Exception) {
            ForageError(
                httpStatusCode = httpStatusCode,
                code = "",
                message = httpStatusCode.toString(),
                details = null
            )
        }
    }

    private fun toForageError(httpStatusCode: Int, errorPayload: ErrorPayload) = ForageError(
        httpStatusCode,
        code = errorPayload.parseCode(),
        message = errorPayload.parseMessage(),
        details = errorPayload.parseDetails()
    )

    private fun parseJson(jsonStringErrorResponse: String): ErrorPayload =
        parseJson(JSONObject(jsonStringErrorResponse))

    protected abstract fun parseJson(jsonErrorResponse: JSONObject): ErrorPayload
}
