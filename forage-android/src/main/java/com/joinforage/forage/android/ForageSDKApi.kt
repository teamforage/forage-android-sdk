package com.joinforage.forage.android

import android.content.Context
import com.joinforage.forage.android.network.model.ResponseListener
import com.joinforage.forage.android.ui.ForagePINEditText

internal interface ForageSDKApi {
    fun tokenizeEBTCard(
        merchantAccount: String,
        bearerToken: String,
        responseCallback: ResponseListener
    )

    fun checkBalance(
        context: Context,
        pinForageEditText: ForagePINEditText,
        merchantAccount: String,
        bearerToken: String,
        paymentMethodRef: String,
        cardToken: String,
        idempotencyKey: String,
        onResponseListener: ResponseListener
    )

    fun capturePayment(
        context: Context,
        pinForageEditText: ForagePINEditText,
        merchantAccount: String,
        bearerToken: String,
        paymentRef: String,
        cardToken: String,
        idempotencyKey: String,
        onResponseListener: ResponseListener
    )
}
