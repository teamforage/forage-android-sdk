package com.joinforage.forage.android.network.model

import org.json.JSONObject

sealed class ForageApiResponse<out T> {
    data class Success<out T>(val data: T) : ForageApiResponse<T>()

    data class Failure(val status: Int, val code: String, val message: String) : ForageApiResponse<Nothing>()
}

data class ForageError(
    val code: String,
    val message: String
) {
    object ForageErrorMapper {
        fun from(string: String): ForageError {
            val jsonObject = JSONObject(string)

            val code = jsonObject.getString("code")
            val message = jsonObject.getString("message")

            return ForageError(
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
    val errors: List<ForageError>
) {
    object ForageApiErrorMapper {
        fun from(string: String): ForageApiError {
            val jsonObject = JSONObject(string)

            val path = jsonObject.getString("path")
            val errors = jsonObject.optJSONArray("errors")
                ?.let { 0.until(it.length()).map { i -> it.optJSONObject(i) } } // returns an array of JSONObject
                ?.map { ForageError.ForageErrorMapper.from(it.toString()) } // transforms each JSONObject of the array into ForageError
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
