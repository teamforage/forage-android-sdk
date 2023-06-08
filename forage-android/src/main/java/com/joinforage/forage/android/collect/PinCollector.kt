package com.joinforage.forage.android.collect

import com.joinforage.forage.android.network.model.ForageApiResponse

internal interface PinCollector {
    suspend fun collectPinForBalanceCheck(
        paymentMethodRef: String,
        cardToken: String,
        encryptionKey: String,
        merchantAccount: String
    ): ForageApiResponse<String>

    suspend fun collectPinForCapturePayment(
        paymentRef: String,
        cardToken: String,
        encryptionKey: String,
        merchantAccount: String
    ): ForageApiResponse<String>
}
