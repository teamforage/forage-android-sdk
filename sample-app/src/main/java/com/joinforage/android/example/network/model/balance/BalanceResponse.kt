package com.joinforage.android.example.network.model.balance

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BalanceResponse(
    var snap: String,
    var cash: String
)
