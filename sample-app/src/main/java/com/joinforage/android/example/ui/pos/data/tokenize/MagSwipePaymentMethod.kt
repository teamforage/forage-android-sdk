package com.joinforage.android.example.ui.pos.data.tokenize

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MagSwipePaymentMethod(
    val card: Card,
    val type: String = "ebt",
    val reusable: Boolean = true
) {
    @JsonClass(generateAdapter = true)
    data class Card(
        @Json(name = "track_2_data") val track2Data: String
    )

    constructor(track2Data: String) : this(Card(track2Data))
}
