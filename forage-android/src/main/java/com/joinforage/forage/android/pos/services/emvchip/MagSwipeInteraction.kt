package com.joinforage.forage.android.pos.services.emvchip

data class MagSwipeInteraction(
    override val track2Data: String
) : CardholderInteraction {
    override val rawPan: String? = null
    override val type = CardholderInteractionType.MagSwipe
}
