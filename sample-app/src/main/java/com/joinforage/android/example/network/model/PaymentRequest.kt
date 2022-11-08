package com.joinforage.android.example.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PaymentRequest(
    val amount: Long,
    @Json(name = "funding_type")
    val fundingType: String,
    @Json(name = "payment_method")
    val paymentMethod: String,
    val description: String,
    val metadata: Map<String, String>,
    @Json(name = "delivery_address")
    val deliveryAddress: Address,
    @Json(name = "is_delivery")
    val isDelivery: Boolean
)
