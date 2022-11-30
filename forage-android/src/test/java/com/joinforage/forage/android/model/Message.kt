package com.joinforage.forage.android.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Message(
    @Json(name = "content_id")
    val contentId: String,
    @Json(name = "message_type")
    val messageType: String,
    val status: String,
    val failed: Boolean,
    val errors: List<Error>
)
