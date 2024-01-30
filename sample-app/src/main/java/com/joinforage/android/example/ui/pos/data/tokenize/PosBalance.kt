package com.joinforage.android.example.ui.pos.data.tokenize

import com.joinforage.android.example.ui.pos.data.PosTerminal
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PosBalance(
    val snap: String,
    val non_snap: String,
    val updated: String,
    @Json(name = "pos_terminal")
    val posTerminal: PosTerminal?,
    @Json(name = "sequence_number")
    val sequenceNumber: String? = null
)

