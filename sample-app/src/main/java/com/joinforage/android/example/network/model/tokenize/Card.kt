package com.joinforage.android.example.network.model.tokenize

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Card(
    @Json(name = "last_4")
    val last4: String,
    val created: String,
    val token: String
)