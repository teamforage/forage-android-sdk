package com.joinforage.forage.android.network.model

import org.json.JSONObject

data class Message(
    val contentId: String,
    val messageType: String,
    val status: String,
    val failed: Boolean,
    val errors: List<String>
) {
    object ModelMapper {
        fun from(string: String): Message {
            val jsonObject = JSONObject(string)

            val contentId = jsonObject.getString("content_id")
            val messageType = jsonObject.getString("message_type")
            val status = jsonObject.getString("status")
            val failed = jsonObject.getBoolean("failed")

            return Message(
                contentId = contentId,
                messageType = messageType,
                status = status,
                failed = failed,
                errors = emptyList()
            )
        }
    }
}
