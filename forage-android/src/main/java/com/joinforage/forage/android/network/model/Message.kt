package com.joinforage.forage.android.network.model

import org.json.JSONObject

sealed class ForageErrorDetails {
    data class EbtError51Details(val snapBalance: String? = null, val cashBalance: String? = null) : ForageErrorDetails() {
        companion object {
            fun from(detailsJson: JSONObject?): EbtError51Details {
                // TODO: should probably add a log here if detailsJSON
                //  is null since it should not be null if this is called
                val snapBalance = detailsJson?.optString("snap_balance", null)
                val cashBalance = detailsJson?.optString("cash_balance", null)
                return EbtError51Details(snapBalance, cashBalance)
            }
        }

        override fun toString(): String = "Cash Balance: $cashBalance\nSNAP Balance: $snapBalance"
    }
}

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
            val rawDetails = jsonObject.optJSONObject("details")

            val parsedDetails = when (forageCode) {
                "ebt_error_51" -> ForageErrorDetails.EbtError51Details.from(rawDetails)
                else -> null
            }

            return SQSError(
                statusCode = statusCode,
                forageCode = forageCode,
                message = message,
                details = parsedDetails
            )
        }
    }

    fun toForageError(): ForageApiResponse.Failure {
        val forageError = ForageError(
            httpStatusCode = statusCode,
            code = forageCode,
            message = message,
            details = details
        )
        return ForageApiResponse.Failure(listOf(forageError))
    }
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
