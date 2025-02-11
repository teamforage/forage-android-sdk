package com.joinforage.forage.android.core.services.forageapi.network.error

import com.joinforage.forage.android.core.services.forageapi.network.error.payload.ErrorPayload
import org.json.JSONObject

internal abstract class ForageErrorResponseParser {
    internal fun toForageError(httpStatusCode: Int, jsonString: String) =
        toForageError(httpStatusCode, parseJson(jsonString))

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
