package com.joinforage.forage.android.pos

import com.joinforage.forage.android.network.data.BaseVaultRequestParams

internal data class PosVaultRequestParams(
    override val cardNumberToken: String,
    override val encryptionKey: String,
    val posTerminalId: String
) : BaseVaultRequestParams(cardNumberToken, encryptionKey) {
    override fun equals(other: Any?): Boolean {
        return super.equals(other) && other is PosVaultRequestParams && posTerminalId == other.posTerminalId
    }

    override fun hashCode(): Int {
        return super.hashCode() + posTerminalId.hashCode()
    }
}
