package com.joinforage.forage.android.pos.services.emvchip

data class MagSwipeInteraction(
    override val track2Data: String
) : CardholderInteraction {
    override val rawPan: String = Track2Parser(track2Data).rawPan
    override val type = CardholderInteractionType.MagSwipe
}

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
