package com.joinforage.forage.android.pos.services.emvchip

class MagSwipeInteraction(
    track2Data: String
) : CardInteraction(track2Data) {
    override val type: CardholderInteractionType = parser.interactionType
    override val emvField55Data: String? = null
}
