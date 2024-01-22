package com.joinforage.android.example.ui.pos.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BalanceCheck(
    val snap: String,
    val cash: String
)
