package com.joinforage.android.example.ui.pos.data.tokenize

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PosPaymentMethod(
    val ref: String,
    val type: String,
    val reusable: Boolean,
    val card: PosCard?,
    val balance: PosBalance?,
    @Json(name = "customer_id")
    val customerId: String? = null
)
