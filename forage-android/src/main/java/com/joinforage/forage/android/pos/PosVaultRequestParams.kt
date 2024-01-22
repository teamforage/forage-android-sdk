package com.joinforage.forage.android.pos

import com.joinforage.forage.android.network.data.BaseVaultRequestParams
import com.joinforage.forage.android.vault.VaultSubmitterParams

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

internal data class PosBalanceVaultSubmitterParams(
    val baseVaultSubmitterParams: VaultSubmitterParams,
    val posTerminalId: String
) : VaultSubmitterParams(
    encryptionKeys = baseVaultSubmitterParams.encryptionKeys,
    idempotencyKey = baseVaultSubmitterParams.idempotencyKey,
    merchantId = baseVaultSubmitterParams.merchantId,
    path = baseVaultSubmitterParams.path,
    paymentMethod = baseVaultSubmitterParams.paymentMethod,
    userAction = baseVaultSubmitterParams.userAction,
    sessionToken = baseVaultSubmitterParams.sessionToken
)

internal data class PosRefundVaultSubmitterParams(
    val baseVaultSubmitterParams: VaultSubmitterParams,
    val posTerminalId: String,
    val refundParams: PosRefundPaymentParams
) : VaultSubmitterParams(
    encryptionKeys = baseVaultSubmitterParams.encryptionKeys,
    idempotencyKey = baseVaultSubmitterParams.idempotencyKey,
    merchantId = baseVaultSubmitterParams.merchantId,
    path = baseVaultSubmitterParams.path,
    paymentMethod = baseVaultSubmitterParams.paymentMethod,
    userAction = baseVaultSubmitterParams.userAction,
    sessionToken = baseVaultSubmitterParams.sessionToken
)
