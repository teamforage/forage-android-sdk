package com.joinforage.android.example.network.model

import com.squareup.moshi.JsonClass
import java.io.Serializable

@JsonClass(generateAdapter = true)
data class Address(
    val city: String,
    val country: String,
    val line1: String,
    val line2: String,
    val zipcode: String,
    val state: String
) : Serializable
