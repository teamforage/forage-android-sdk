package com.joinforage.forage.android.pos.services.emvchip

enum class CardholderInteractionType(val value: String) {
    KeyEntry("manual"),
    MagSwipeLegacy("magstripe"), 
    MagSwipeFallback("magstripe_fallback"),
    Insert("insert"),
    Tap("tap"),
    Unknown("unknown")
}
