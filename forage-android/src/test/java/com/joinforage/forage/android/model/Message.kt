package com.joinforage.forage.android.model

import com.joinforage.forage.android.network.model.SQSError
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class Message(
    @Json(name = "content_id")
    val contentId: String,
    @Json(name = "message_type")
    val messageType: String,
    val status: String,
    val failed: Boolean,
    val errors: List<SQSError>
)
