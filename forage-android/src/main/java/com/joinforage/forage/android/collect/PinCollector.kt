package com.joinforage.forage.android.collect

import com.joinforage.forage.android.VaultType
import com.joinforage.forage.android.model.EncryptionKeys
import com.joinforage.forage.android.model.PaymentMethod
import com.joinforage.forage.android.network.data.BaseVaultRequestParams
import com.joinforage.forage.android.network.model.ForageApiResponse

internal object CollectorConstants {
    const val TOKEN_DELIMITER = ","
}

internal interface PinCollector {
    suspend fun <T : BaseVaultRequestParams>submitBalanceCheck(
        paymentMethodRef: String,
        vaultRequestParams: T
    ): ForageApiResponse<String>

    suspend fun <T : BaseVaultRequestParams>submitPaymentCapture(
        paymentRef: String,
        vaultRequestParams: T
    ): ForageApiResponse<String>

    suspend fun <T : BaseVaultRequestParams>submitDeferPaymentCapture(
        paymentRef: String,
        vaultRequestParams: T
    ): ForageApiResponse<String>

    fun parseEncryptionKey(
        encryptionKeys: EncryptionKeys
    ): String

    fun parseVaultToken(
        paymentMethod: PaymentMethod
    ): String

    fun getVaultType(): VaultType
}
