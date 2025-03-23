package com.joinforage.forage.android.pos.services.emvchip

/**
 * Represents the details captured during a magnetic swipe card interaction.
 *
 * This data class is used specifically for scenarios where the card is swiped
 * through a magnetic stripe reader. It is not applicable for other interaction
 * types such as tap or insert.
 */
class MagSwipeInteraction(
    track2Data: String
) : CardholderInteraction(
    track2Data = track2Data,
    type = CardholderInteractionType.MagSwipe,
    rawPan = Track2Parser(track2Data).rawPan
)

private class Track2Parser(track2Data: String) {
    val rawPan: String = parsePan(track2Data)

    private fun parsePan(data: String): String {
        val panStartIndex = data.indexOf(';') + 1
        val equalsIndex = data.indexOf('=')
        if (equalsIndex <= panStartIndex) {
            // the track2 data is malformed but we'll
            // let the server enforce this since its
            // easier to hotfix a server than a client.
            // So, let's just continue with bad data
            return data
        }
        return data.substring(panStartIndex, equalsIndex)
    }
}
