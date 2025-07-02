package com.joinforage.android.example.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AuthorizeRequest(
    @Json(name = "request_partial_authorization") val requestPartialAuthorization: Boolean
)
