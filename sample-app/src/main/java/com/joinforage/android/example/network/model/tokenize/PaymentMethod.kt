package com.joinforage.android.example.network.model.tokenize

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PaymentMethod(
    val ref: String,
    val type: String,
    val reusable: Boolean,
    val card: Card?,
    val balance: Balance?,
    @Json(name = "customer_id")
    val customerId: String?
)
