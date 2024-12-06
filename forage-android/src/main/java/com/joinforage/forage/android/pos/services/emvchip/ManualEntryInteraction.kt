package com.joinforage.forage.android.pos.services.emvchip

class ManualEntryInteraction(
    override val rawPan: String
) : CardholderInteraction {
    override val type = CardholderInteractionType.KeyEntry
    override val track2Data: String? = null
    override val emvField55Data: String? = null
}
