package com.joinforage.forage.android

import android.content.Context
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ResponseListener
import com.joinforage.forage.android.ui.ForagePINEditText

internal interface ForageSDKApi {
    suspend fun tokenizeEBTCard(
        merchantAccount: String,
        bearerToken: String
    ): ForageApiResponse<String>

    suspend fun checkBalance(
        context: Context,
        pinForageEditText: ForagePINEditText,
        merchantAccount: String,
        bearerToken: String,
        paymentMethodRef: String,
        cardToken: String
    ): ForageApiResponse<String>

    fun capturePayment(
        context: Context,
        pinForageEditText: ForagePINEditText,
        merchantAccount: String,
        bearerToken: String,
        paymentRef: String,
        cardToken: String,
        onResponseListener: ResponseListener
    )
}
