package com.joinforage.forage.android.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Error(
    val code: String,
    val message: String
)
