package com.joinforage.forage.android.collect

import com.joinforage.forage.android.VaultType
import com.joinforage.forage.android.model.EncryptionKeys
import com.joinforage.forage.android.model.PaymentMethod
import com.joinforage.forage.android.network.data.BaseVaultRequestParams
import com.joinforage.forage.android.network.model.ForageApiResponse

internal object CollectorConstants {
    const val TOKEN_DELIMITER = ","
}

@Deprecated(
    message = "Use VaultSubmitter instead",
    replaceWith = ReplaceWith(
        expression = "VaultSubmitter",
        imports = ["com.joinforage.forage.android.collect.VaultSubmitter"]
    ),
    level = DeprecationLevel.WARNING
)
internal interface PinCollector {
    suspend fun submitBalanceCheck(
        paymentMethodRef: String,
        vaultRequestParams: BaseVaultRequestParams
    ): ForageApiResponse<String>

    suspend fun submitPaymentCapture(
        paymentRef: String,
        vaultRequestParams: BaseVaultRequestParams
    ): ForageApiResponse<String>

    suspend fun submitDeferPaymentCapture(
        paymentRef: String,
        vaultRequestParams: BaseVaultRequestParams
    ): ForageApiResponse<String>

    fun parseEncryptionKey(
        encryptionKeys: EncryptionKeys
    ): String

    fun parseVaultToken(
        paymentMethod: PaymentMethod
    ): String

    fun getVaultType(): VaultType
}
