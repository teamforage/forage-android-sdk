package com.joinforage.forage.android.core.services.forageapi.network.error

import com.joinforage.forage.android.core.services.forageapi.network.error.payload.ErrorPayload

/**
 * A model that represents an error response from the Forage API.
 *
 * @property httpStatusCode A number that corresponds to the HTTP status code that the Forage API
 * returns in response to the request.
 * @property code A short string that helps identify the cause of the error.
 * For example, [`ebt_error_55`](https://docs.joinforage.app/reference/errors#ebt_error_55)
 * indicates that a customer entered an invalid EBT Card PIN.
 * @property message A string that specifies developer-facing error handling instructions.
 * @property details An object that includes additional details about the error, when available, like for
 * [`ebt_error_51`](https://docs.joinforage.app/reference/errors#ebt_error_51) (Insufficient Funds).
 * @see [SDK Errors](https://docs.joinforage.app/reference/errors#sdk-errors) for a comprehensive
 * list of error `code` and `message` pairs.
 *
 */
data class ForageError internal constructor(
    val httpStatusCode: Int,
    val code: String,
    val message: String,
    val details: ForageErrorDetails? = null
) {

    internal constructor(httpStatusCode: Int, jsonString: String) : this(
        httpStatusCode,
        ErrorPayload.parseJsonString(jsonString)
    )

    internal constructor(httpStatusCode: Int, errorPayload: ErrorPayload) : this(
        httpStatusCode,
        code = errorPayload.parseCode(),
        message = errorPayload.parseMessage(),
        details = errorPayload.parseDetails()
    )

    /**
     * A method that converts the [ForageError] response to a string.
     *
     * @return A string representation of a [ForageError] instance, including code, message,
     * HTTP status, and any other available details from the Forage API.
     */
    override fun toString() = "" +
        "Code: ${code}\n" +
        "Message: ${message}\n" +
        "Status Code: $httpStatusCode\n" +
        "Error Details (below):\n$details"
}
