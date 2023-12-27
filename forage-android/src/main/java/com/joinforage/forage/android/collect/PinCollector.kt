package com.joinforage.forage.android.collect

import com.joinforage.forage.android.VaultType
import com.joinforage.forage.android.model.EncryptionKeys
import com.joinforage.forage.android.model.PaymentMethod
import com.joinforage.forage.android.network.model.ForageApiResponse

internal object CollectorConstants {
    const val TOKEN_DELIMITER = ","
}

internal interface PinCollector {
    suspend fun submitBalanceCheck(
        paymentMethodRef: String,
        cardToken: String,
        encryptionKey: String
    ): ForageApiResponse<String>

    suspend fun submitPaymentCapture(
        paymentRef: String,
        cardToken: String,
        encryptionKey: String
    ): ForageApiResponse<String>

    suspend fun submitDeferPaymentCapture(
        paymentRef: String,
        cardToken: String,
        encryptionKey: String
    ): ForageApiResponse<String>

    fun parseEncryptionKey(
        encryptionKeys: EncryptionKeys
    ): String

    fun parseVaultToken(
        paymentMethod: PaymentMethod
    ): String

    fun getVaultType(): VaultType
}
