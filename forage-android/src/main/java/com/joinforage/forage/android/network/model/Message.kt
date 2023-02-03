package com.joinforage.forage.android.network.model

import org.json.JSONObject

data class SQSError(
    val statusCode: Int,
    val forageCode: String,
    val message: String
) {
    object SQSErrorMapper {
        fun from(string: String): SQSError {
            val jsonObject = JSONObject(string)

            val statusCode = jsonObject.getInt("status_code")
            val forageCode = jsonObject.getString("forage_code")
            val message = jsonObject.getString("message")

            return SQSError(
                statusCode = statusCode,
                forageCode = forageCode,
                message = message
            )
        }
    }
}

data class Message(
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
                ?.map { SQSError.SQSErrorMapper.from(it.toString()) } // transforms each JSONObject of the array into SQSError
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
