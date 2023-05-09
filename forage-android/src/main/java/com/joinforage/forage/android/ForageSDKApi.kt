package com.joinforage.forage.android

import android.content.Context
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.ui.ForagePINEditText

/**
 * The Forage SKD public API
 */
internal interface ForageSDKApi {
    suspend fun tokenizeEBTCard(
        merchantAccount: String,
        bearerToken: String,
        userId: String
    ): ForageApiResponse<String>

    suspend fun checkBalance(
        context: Context,
        pinForageEditText: ForagePINEditText,
        merchantAccount: String,
        bearerToken: String,
        paymentMethodRef: String,
        cardToken: String
    ): ForageApiResponse<String>

    suspend fun capturePayment(
        context: Context,
        pinForageEditText: ForagePINEditText,
        merchantAccount: String,
        bearerToken: String,
        paymentRef: String,
        cardToken: String
    ): ForageApiResponse<String>
}
