package com.joinforage.forage.android.pos.services.emvchip

abstract class CardInteraction(
    final override val track2Data: String
) : CardholderInteraction {
    protected val parser = Track2Parser(track2Data)
    override val rawPan: String = parser.rawPan
}