package com.joinforage.android.example.ui.pos.data.tokenize

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PosCard(
    @Json(name = "last_4")
    val last4: String,
    val created: String,
    val token: String,
    val state: String?,
    val number: String
)
