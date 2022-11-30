package com.joinforage.android.example.network.model.balance

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Balance(
    val snap: String,
    @Json(name = "non_snap")
    val nonSnap: String
)
