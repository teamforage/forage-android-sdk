package com.joinforage.android.example.ui.pos.data.tokenize

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PosTerminalResponseField(
    @Json(name = "terminal_id")
    val terminalId: String,
    @Json(name = "provider_terminal_id")
    val providerTerminalId: String
)
