package com.joinforage.forage.android.pos.services.vault.rosetta

import com.joinforage.forage.android.core.services.vault.VaultSubmitterParams
import com.joinforage.forage.android.pos.services.RefundPaymentParams

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
    val refundParams: RefundPaymentParams
) : VaultSubmitterParams(
    encryptionKeys = baseVaultSubmitterParams.encryptionKeys,
    idempotencyKey = baseVaultSubmitterParams.idempotencyKey,
    merchantId = baseVaultSubmitterParams.merchantId,
    path = baseVaultSubmitterParams.path,
    paymentMethod = baseVaultSubmitterParams.paymentMethod,
    userAction = baseVaultSubmitterParams.userAction,
    sessionToken = baseVaultSubmitterParams.sessionToken
)
