package com.joinforage.android.example.network.model.balance

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BalanceResponse(
    @Json(name = "non_snap")
    val nonSnap: String,
    val snap: String
)
