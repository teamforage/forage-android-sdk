package com.joinforage.forage.android.pos.services.vault.rosetta

import com.joinforage.forage.android.core.services.vault.VaultSubmitterParams
import com.joinforage.forage.android.pos.services.forageapi.paymentmethod.PosRefundPaymentParams


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
