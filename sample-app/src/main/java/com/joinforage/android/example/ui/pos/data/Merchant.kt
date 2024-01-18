package com.joinforage.android.example.ui.pos.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Merchant (
    val ref: String,
    val name: String,
    @Json(name = "internal_name") val internalName: String,
    val fns: String,
    val address: Address?,
    val logo: String,
    val homepage: String,
    @Json(ignore = true, name = "third_party_api_keys") internal val thirdPartyApiKeys: Any? = null,
    @Json(name = "possible_supported_benefits") val possibleSupportedBenefits: List<String>,
    @Json(name = "uses_shopify") val usesShopify: Boolean,
)

@JsonClass(generateAdapter = true)
data class Address (
    val city: String,
    val country: String,
    val line1: String,
    val line2: String?,
    val state: String,
    val zipcode: String,
)
