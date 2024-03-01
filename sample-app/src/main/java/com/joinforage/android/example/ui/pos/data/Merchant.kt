package com.joinforage.android.example.ui.pos.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Merchant(
    val ref: String,
    val name: String,
    val fns: String,
    val address: Address?
)

@JsonClass(generateAdapter = true)
data class Address(
    val city: String,
    val country: String,
    val line1: String,
    val line2: String?,
    val state: String,
    val zipcode: String
)
