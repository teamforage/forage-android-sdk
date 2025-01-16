package com.joinforage.forage.android.pos.services.emvchip

/**
 * Represents the details captured during a magnetic swipe card interaction.
 *
 * This data class is used specifically for scenarios where the card is swiped * through a magnetic stripe reader. It is not applicable for other interaction * types such as tap or insert.
 */
data class MagSwipeInteraction(
    override val track2Data: String
) : CardholderInteraction {
    override val rawPan: String? = null
    override val type = CardholderInteractionType.MagSwipe
}
