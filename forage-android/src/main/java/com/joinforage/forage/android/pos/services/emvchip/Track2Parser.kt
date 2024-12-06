package com.joinforage.forage.android.pos.services.emvchip

class Track2Parser(track2Data: String) {
    internal val rawPan: String
    internal val interactionType: CardholderInteractionType

    init {
        rawPan = parsePan(track2Data)
        val serviceCode = parseServiceCode(track2Data)
        interactionType = getMagSwipeInteractionType(serviceCode)
    }

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

    private fun parseServiceCode(data: String): String {
        val equalsIndex = data.indexOf('=')
        val serviceCodeStartIndex = equalsIndex + 5
        val serviceCodeEndIndex = serviceCodeStartIndex + 3

        if (serviceCodeStartIndex + 3 > data.length) {
            // the track2 data is malformed but we'll
            // let the server enforce this since its
            // easier to hotfix a server than a client.
            // So, let's just continue with bad data
            return data
        }
        return data.substring(serviceCodeStartIndex, serviceCodeEndIndex)
    }

    private fun getMagSwipeInteractionType(serviceCode: String): CardholderInteractionType =
        when (serviceCode) {
            "120" -> CardholderInteractionType.MagSwipeLegacy
            "220" -> CardholderInteractionType.MagSwipeFallback
            else -> CardholderInteractionType.Unknown
        }

}