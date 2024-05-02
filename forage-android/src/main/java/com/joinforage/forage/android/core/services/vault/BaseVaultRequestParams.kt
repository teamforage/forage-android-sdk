package com.joinforage.forage.android.core.services.vault

internal open class BaseVaultRequestParams(
    open val cardNumberToken: String,
    open val encryptionKey: String
) {
    override fun equals(other: Any?): Boolean {
        return other is BaseVaultRequestParams &&
            other.cardNumberToken == cardNumberToken &&
            other.encryptionKey == encryptionKey
    }

    override fun hashCode(): Int {
        return cardNumberToken.hashCode() + encryptionKey.hashCode()
    }
}
