package com.joinforage.forage.android.collect

import com.joinforage.forage.android.model.EncryptionKey
import com.joinforage.forage.android.model.PaymentMethod
import com.joinforage.forage.android.network.model.ForageApiResponse

internal object CollectorConstants {
    const val TOKEN_DELIMITER = ","
}

internal interface PinCollector {
    suspend fun collectPinForBalanceCheck(
        paymentMethodRef: String,
        cardToken: String,
        encryptionKey: String
    ): ForageApiResponse<String>

    suspend fun collectPinForCapturePayment(
        paymentRef: String,
        cardToken: String,
        encryptionKey: String
    ): ForageApiResponse<String>

    fun parseEncryptionKey(
        encryptionKeys: EncryptionKey
    ): String

    fun parseVaultToken(
        paymentMethod: PaymentMethod
    ): String
}
