package com.joinforage.forage.android.pos.services.emvchip

class InsertEMVInteraction(track2Data: String, emvField55Data: String) : EMVInteraction(track2Data, emvField55Data,
    CardholderInteractionType.Insert
)