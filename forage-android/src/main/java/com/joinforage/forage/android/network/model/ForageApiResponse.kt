package com.joinforage.forage.android.network.model

import org.json.JSONObject

internal val UnknownErrorApiResponse = ForageApiResponse.Failure.fromError(
    ForageError(500, "unknown_server_error", "Unknown Server Error")
)

sealed class ForageApiResponse<out T> {
    data class Success<out T>(val data: T) : ForageApiResponse<T>()

    data class Failure(val errors: List<ForageError>) : ForageApiResponse<Nothing>() {
        companion object {
            fun fromError(error: ForageError): Failure {
                return Failure(listOf(error))
            }
        }
    }
}

// Learn more about `ForageError`s [here](https://docs.joinforage.app/reference/forage-js-errors#forageerror)
data class ForageError(
    // The HTTP status that the Forage API returns in response to the request.
    val httpStatusCode: Int,

    // A short string explaining why the request failed. The [error code](https://docs.joinforage.app/reference/errors#error-codes)
    // string corresponds to the HTTP status code.
    val code: String,

    // A developer-facing message about the error, not to be displayed to customers.
    val message: String,

    // Additional data associated with certain ForageErrors included for your
    // convenience. Guaranteed to be present for ForageErrors with details
    // (e.g. error_code_51 Insufficient Balance). null for all other ForageErrors
    val details: ForageErrorDetails? = null
) {
    override fun toString(): String {
        return "Code: $code\nMessage: $message\nStatus Code: $httpStatusCode\nError Details (below):\n$details"
    }
}

data class ForageErrorObj(
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

data class ForageApiError(
    val path: String,
    val errors: List<ForageErrorObj>
) {
    object ForageApiErrorMapper {
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
