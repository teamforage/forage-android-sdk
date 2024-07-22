package com.joinforage.forage.android.core.services.forageapi.polling

import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.network.error.ForageErrorDetails
import org.json.JSONObject

internal data class SQSError(
    val statusCode: Int,
    val forageCode: String,
    val message: String,
    val details: ForageErrorDetails? = null
) {
    companion object SQSErrorMapper {
        fun from(jsonString: String): SQSError {
            val jsonObject = JSONObject(jsonString)

            val statusCode = jsonObject.getInt("status_code")
            val forageCode = jsonObject.getString("forage_code")
            val message = jsonObject.getString("message")
            val details = ForageErrorDetails.from(forageCode, jsonObject)

            return SQSError(
                statusCode = statusCode,
                forageCode = forageCode,
                message = message,
                details = details
            )
        }
    }

    fun toForageError() = ForageApiResponse.Failure(
        httpStatusCode = statusCode,
        code = forageCode,
        message = message,
        details = details
    )
}

internal data class Message(
    val contentId: String,
    val messageType: String,
    val status: String,
    val failed: Boolean,
    val errors: List<SQSError>
) {
    object ModelMapper {
        fun from(string: String): Message {
            val jsonObject = JSONObject(string)

            val contentId = jsonObject.getString("content_id")
            val messageType = jsonObject.getString("message_type")
            val status = jsonObject.getString("status")
            val failed = jsonObject.getBoolean("failed")
            val errors = jsonObject.optJSONArray("errors")
                ?.let { 0.until(it.length()).map { i -> it.optJSONObject(i) } } // returns an array of JSONObject
                ?.map { SQSError.from(it.toString()) } // transforms each JSONObject of the array into SQSError
                ?: return Message(
                    contentId = contentId,
                    messageType = messageType,
                    status = status,
                    failed = failed,
                    errors = emptyList()
                )

            return Message(
                contentId = contentId,
                messageType = messageType,
                status = status,
                failed = failed,
                errors = errors
            )
        }
    }
}
