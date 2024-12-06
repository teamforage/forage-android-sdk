package com.joinforage.forage.android.pos.services

interface CardholderInteraction {
    val rawPan: String
    val type: CardholderInteractionType
    val track2Data: String?
    val emvField55Data: String?
}

enum class CardholderInteractionType {
    KeyEntry,
    MagSwipeLegacy,
    MagSwipeFallback,
    Insert,
    Tap
}

class ManualEntryInteraction(
    override val rawPan: String
) : CardholderInteraction {
    override val type = CardholderInteractionType.KeyEntry
    override val track2Data: String? = null
    override val emvField55Data: String? = null
}

abstract class CardInteraction(
    final override val track2Data: String
) : CardholderInteraction {
    protected val parser = Track2Parser(track2Data)
    override val rawPan: String = parser.rawPan
}

class MagSwipeInteraction(
    track2Data: String
) : CardInteraction(track2Data) {
    override val type: CardholderInteractionType = parser.interactionType
    override val emvField55Data: String? = null
}

abstract class EMVInteraction(
    track2Data: String,
    override val emvField55Data: String,
    override val type: CardholderInteractionType
) : CardInteraction(track2Data)

class TapEMVInteraction(track2Data: String, emvField55Data: String) : EMVInteraction(track2Data, emvField55Data, CardholderInteractionType.Tap)
class InsertEMVInteraction(track2Data: String, emvField55Data: String) : EMVInteraction(track2Data, emvField55Data, CardholderInteractionType.Insert)

class Track2Parser(track2Data: String) {
    internal val rawPan: String
    internal val serviceCode: String
    internal val interactionType: CardholderInteractionType

    init {
        validateTrack2Data(track2Data)
        rawPan = parsePan(track2Data)
        serviceCode = parseServiceCode(track2Data)
        interactionType = getMagSwipeInteractionType(serviceCode)
    }

    private fun validateTrack2Data(data: String) {
        if (data.isEmpty() || !data.startsWith(';') || !data.contains('=') || data.length < 16) {
            throw MalformedTrack2Exception(data)
        }
    }

    private fun parsePan(data: String): String {
        val panStartIndex = data.indexOf(';') + 1
        val equalsIndex = data.indexOf('=')
        if (equalsIndex <= panStartIndex) {
            throw MalformedTrack2Exception(data)
        }
        return data.substring(panStartIndex, equalsIndex)
    }

    private fun parseServiceCode(data: String): String {
        val equalsIndex = data.indexOf('=')
        val serviceCodeStartIndex = equalsIndex + 5
        val serviceCodeEndIndex = serviceCodeStartIndex + 3

        if (serviceCodeStartIndex + 3 > data.length) {
            throw MalformedTrack2Exception(data)
        }
        return data.substring(serviceCodeStartIndex, serviceCodeEndIndex)
    }

    private fun getMagSwipeInteractionType(serviceCode: String): CardholderInteractionType =
        when (serviceCode) {
            "120" -> CardholderInteractionType.MagSwipeLegacy
            "220" -> CardholderInteractionType.MagSwipeFallback
            else -> throw UnsupportedServiceCodeException(serviceCode)
        }

    class MalformedTrack2Exception(track2Data: String) : Exception("Malformed track 2 data: $track2Data")
    class UnsupportedServiceCodeException(serviceCode: String) : Exception("Unsupported service code in track 2: $serviceCode")
}
