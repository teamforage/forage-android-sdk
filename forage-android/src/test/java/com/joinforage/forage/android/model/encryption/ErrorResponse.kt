package com.joinforage.forage.android.model.encryption

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ErrorResponse(
    val detail: String
)
