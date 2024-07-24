package com.joinforage.forage.android.core.services.forageapi.polling

import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.network.error.ForageError
import org.json.JSONObject

/**
 * A model that is a blueprint for all possible types of `details` returned in a [ForageError]
 * response.
 */
sealed class ForageErrorDetails {
    /**
     * An error that is returned when a customer's EBT Card balance is insufficient to complete a
     * payment.
     * @property snapBalance A string that represents the available SNAP balance on the EBT Card.
     * @property cashBalance A string that represents the available EBT Cash balance on the EBT Card.
     * @see [Forage guide to handling insufficient funds](https://docs.joinforage.app/docs/plan-for-insufficient-ebt-funds-errors)
     */
    data class EbtError51Details(
        val snapBalance: String?,
        val cashBalance: String?
    ) : ForageErrorDetails() {
        internal constructor(detailsJson: JSONObject?) : this(
            detailsJson?.opt("snap_balance") as String?,
            detailsJson?.opt("cash_balance") as String?
        )

        /**
         * A method that converts the [EbtError51Details] to a string.
         *
         * @return A string representation of [EbtError51Details].
         */
        override fun toString(): String = "Cash Balance: $cashBalance\nSNAP Balance: $snapBalance"
    }

    companion object {
        fun from(forageCode: String, jsonForageError: JSONObject?): ForageErrorDetails? {
            val jsonDetails = jsonForageError?.optJSONObject("details") ?: return null
            return when (forageCode) {
                "ebt_error_51" -> EbtError51Details(jsonDetails)
                else -> null
            }
        }
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
