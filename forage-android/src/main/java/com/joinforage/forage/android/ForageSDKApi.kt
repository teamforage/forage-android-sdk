package com.joinforage.forage.android

import android.content.Context
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.ui.ForagePANEditText
import com.joinforage.forage.android.ui.ForagePINEditText

/**
 * The Forage SDK public API
 */
internal interface ForageSDKApi {
    suspend fun tokenizeEBTCard(
        merchantAccount: String,
        foragePanEditText: ForagePANEditText,
        bearerToken: String,
        customerId: String,
        reusable: Boolean = true
    ): ForageApiResponse<String>

    suspend fun checkBalance(
        context: Context,
        pinForageEditText: ForagePINEditText,
        merchantAccount: String,
        bearerToken: String,
        paymentMethodRef: String
    ): ForageApiResponse<String>

    suspend fun capturePayment(
        context: Context,
        pinForageEditText: ForagePINEditText,
        merchantAccount: String,
        bearerToken: String,
        paymentRef: String
    ): ForageApiResponse<String>
}
