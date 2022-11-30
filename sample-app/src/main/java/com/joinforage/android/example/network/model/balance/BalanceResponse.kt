package com.joinforage.android.example.network.model.balance

import com.joinforage.android.example.network.model.tokenize.Card
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BalanceResponse(
    var ref: String?,
    var type: String?,
    var balance: Balance?,
    val card: Card
)
