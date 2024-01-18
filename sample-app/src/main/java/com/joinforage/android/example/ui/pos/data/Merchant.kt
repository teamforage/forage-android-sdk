package com.joinforage.android.example.ui.pos.data

import com.squareup.moshi.Json
import kotlinx.serialization.Serializable

@Serializable
data class Merchant (
    val ref: String,
    val name: String,
    @Json(name = "internal_name") val internalName: String,
    val fns: String,
    val address: Address,
    val logo: String,
    val homepage: String,
    @Json(name = "third_party_api_keys") val thirdPartyApiKeys: ThirdPartyApiKeys,
    @Json(name = "possible_supported_benefits") val possibleSupportedBenefits: List<String>,
    @Json(name = "uses_shopify") val usesShopify: Boolean,
)

@Serializable
data class Address (
    val city: String,
    val country: String,
    val line1: String,
    val line2: String,
    val state: String,
    val zipcode: String,
)

@Serializable
data class ThirdPartyApiKeys (
    val platform: String,
    @Json(name = "publishable_key") val publishableKey: String,
    @Json(name = "restricted_key") val restrictedKey: String,
)