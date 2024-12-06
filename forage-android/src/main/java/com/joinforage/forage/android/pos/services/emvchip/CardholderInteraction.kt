package com.joinforage.forage.android.pos.services.emvchip

interface CardholderInteraction {
    val rawPan: String
    val type: CardholderInteractionType
    val track2Data: String?
    val emvField55Data: String?
}

