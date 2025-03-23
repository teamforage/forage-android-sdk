package com.joinforage.forage.android.pos.services.emvchip

/**
 * Represents the details captured during a manual card entry interaction.
 *
 * This data class is used specifically for scenarios where the customer manually * enters the Primary Account Number (PAN), as opposed to other interaction types * such as magnetic swipe, tap, or insert.
 */
class ManualEntryInteraction(
    rawPan: String
) : CardholderInteraction(
    rawPan = rawPan,
    type = CardholderInteractionType.KeyEntry,
    track2Data = null
)
