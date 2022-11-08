package com.joinforage.forage.android.model

sealed class PanEntry {
    data class Invalid(
        val panNumber: String
    ) : PanEntry()

    data class Valid(
        val panNumber: String
    ) : PanEntry()
}

fun PanEntry.getPanNumber() = when (this) {
    is PanEntry.Valid -> panNumber
    is PanEntry.Invalid -> panNumber
}
