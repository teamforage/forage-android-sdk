package com.joinforage.forage.android.pos.services.vault.submission

internal data class PinTranslationParams(
    val encryptedPinBlock: String,
    val keySerialNumber: String,
    val txnCounter: String
)
