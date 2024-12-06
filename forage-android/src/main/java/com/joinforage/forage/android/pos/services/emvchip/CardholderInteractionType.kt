package com.joinforage.forage.android.pos.services.emvchip

enum class CardholderInteractionType {
    KeyEntry,
    MagSwipeLegacy,
    MagSwipeFallback,
    Insert,
    Tap,
    Unknown,
}