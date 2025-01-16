package com.joinforage.forage.android.pos.services.emvchip

/**
 * Represents the details captured during a manual card entry interaction.
 *
 * This data class is used specifically for scenarios where the customer manually * enters the Primary Account Number (PAN), as opposed to other interaction types * such as magnetic swipe, tap, or insert.
 */
data class ManualEntryInteraction(
    override val rawPan: String
) : CardholderInteraction {
    override val type = CardholderInteractionType.KeyEntry
    override val track2Data: String? = null
}
