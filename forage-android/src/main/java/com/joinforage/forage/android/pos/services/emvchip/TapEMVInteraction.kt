package com.joinforage.forage.android.pos.services.emvchip

class TapEMVInteraction(track2Data: String, emvField55Data: String) : EMVInteraction(track2Data, emvField55Data,
    CardholderInteractionType.Tap
)