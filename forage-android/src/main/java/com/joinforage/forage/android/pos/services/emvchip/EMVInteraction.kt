package com.joinforage.forage.android.pos.services.emvchip

abstract class EMVInteraction(
    track2Data: String,
    override val emvField55Data: String,
    override val type: CardholderInteractionType
) : CardInteraction(track2Data)
