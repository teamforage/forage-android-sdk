package com.joinforage.forage.android.network.model

import org.json.JSONException
import org.json.JSONObject

internal val UnknownErrorApiResponse = ForageApiResponse.Failure.fromError(
    ForageError(500, "unknown_server_error", "Unknown Server Error")
)

/**
 * A model that represents the possible types of responses from the Forage API.
 */
sealed class ForageApiResponse<out T> {
    /**
     * A model that represents a success response from the API.
     *
     * In most cases, `data` is a string representation of a JSON object from the Forage API,
     * for example:
     * `if (response is ForageApiResponse.Success) { response.data // { "ref": "abcde123", ... }`
     */
    data class Success<out T>(val data: T) : ForageApiResponse<T>()

    /**
     * A model that represents a failure response from the API.
     *
     * @property errors A list of [ForageError] instances that you can unpack to programmatically
     * handle the error and display the appropriate
     * [customer-facing message](https://docs.joinforage.app/docs/document-error-messages#template-error-table-for-fns-documentation).
     */
    data class Failure(val errors: List<ForageError>) : ForageApiResponse<Nothing>() {
        companion object {
            fun fromError(error: ForageError): Failure {
                return Failure(listOf(error))
            }
        }
    }
}

/**
 * A model that represents an error response from the Forage API.
 *
 * @property httpStatusCode A number that corresponds to the HTTP status code that the Forage API
 * returns in response to the request.
 * @property code A short string that helps identify the cause of the error.
 * For example, [`ebt_error_55`](https://docs.joinforage.app/reference/errors#ebt_error_55)
 * indicates that a customer entered an invalid EBT Card PIN.
 * @property message A string that specifies developer-facing error handling instructions.
 * @property details A string that includes additional details about the error, when available, like for
 * [`ebt_error_51`](https://docs.joinforage.app/reference/errors#ebt_error_51) (Insufficient Funds).
 * @see [SDK Errors](https://docs.joinforage.app/reference/errors#sdk-errors) for a comprehensive
 * list of error `code` and `message` pairs.
 *
 */
data class ForageError(
    val httpStatusCode: Int,
    val code: String,
    val message: String,
    val details: ForageErrorDetails? = null
) {
    /**
     * A function that converts the [ForageError] response to a string.
     *
     * @return A string representation of a [ForageError] instance, including code, message,
     * HTTP status, and any other available details from the Forage API.
     */
    override fun toString(): String {
        return "Code: $code\nMessage: $message\nStatus Code: $httpStatusCode\nError Details (below):\n$details"
    }
}

internal data class ForageErrorObj(
    val code: String,
    val message: String
) {
    object ForageErrorObjMapper {
        fun from(string: String): ForageErrorObj {
            val jsonObject = JSONObject(string)

            val code = jsonObject.getString("code")
            val message = jsonObject.getString("message")

            return ForageErrorObj(
                code = code,
                message = message
            )
        }
    }

    override fun toString(): String {
        return "Code: $code\nMessage: $message"
    }
}

internal data class ForageApiError(
    val path: String,
    val errors: List<ForageErrorObj>
) {
    object ForageApiErrorMapper {
        /**
         * @throws [JSONException] if the `string` is not a valid ForageApiError JSON string
         */
        fun from(string: String): ForageApiError {
            val jsonObject = JSONObject(string)

            val path = jsonObject.getString("path")
            val errors = jsonObject.optJSONArray("errors")
                ?.let { 0.until(it.length()).map { i -> it.optJSONObject(i) } } // returns an array of JSONObject
                ?.map { ForageErrorObj.ForageErrorObjMapper.from(it.toString()) } // transforms each JSONObject of the array into ForageError
                ?: return ForageApiError(
                    path = path,
                    errors = emptyList()
                )

            return ForageApiError(
                path = path,
                errors = errors
            )
        }
    }

    override fun toString(): String {
        if (errors.isEmpty()) {
            return "Path: $path"
        }
        return errors[0].toString()
    }
}
