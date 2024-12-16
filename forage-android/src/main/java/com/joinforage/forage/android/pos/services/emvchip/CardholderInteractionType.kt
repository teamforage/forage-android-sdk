package com.joinforage.forage.android.pos.services.emvchip

enum class CardholderInteractionType(val value: String) {
    KeyEntry("manual"),
    MagSwipe("magstripe"),
    Unknown("unknown")
}
