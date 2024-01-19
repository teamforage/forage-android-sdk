package com.joinforage.forage.android.pos

import com.joinforage.forage.android.collect.VaultProxyRequest
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

internal data class PosVaultProxyRequest(
    override val headers: Map<String, String>,
    override val path: String,
    override val vaultToken: String,
    val posTerminalId: String
) : VaultProxyRequest(
    headers = headers,
    path = path,
    vaultToken = vaultToken
) {
    fun setPosTerminalId(posTerminalId: String) = PosVaultProxyRequest(
        headers = headers,
        path = path,
        vaultToken = vaultToken,
        posTerminalId = posTerminalId
    )
}
