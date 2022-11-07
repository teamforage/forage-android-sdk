package com.joinforage.android.example.network.model.tokenize

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Balance(
    val snap: String,
    val non_snap: String
//    val updated: Date
)
