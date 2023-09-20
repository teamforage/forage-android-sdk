package com.joinforage.forage.android

import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.ui.ForagePANEditText
import com.joinforage.forage.android.ui.ForagePINEditText

/**
 * The Forage SDK public API
 */
internal interface ForageSDKInterface {

    suspend fun tokenizeEBTCard(params: TokenizeEBTCardParams): ForageApiResponse<String>

    suspend fun checkBalance(params: CheckBalanceParams): ForageApiResponse<String>

    suspend fun capturePayment(params: CapturePaymentParams): ForageApiResponse<String>
}

data class TokenizeEBTCardParams(
    val foragePANEditText: ForagePANEditText,
    val customerId: String,
    val reusable: Boolean
)

data class CheckBalanceParams(
    val foragePinEditText: ForagePINEditText,
    val paymentMethodRef: String
)

data class CapturePaymentParams(
    val foragePinEditText: ForagePINEditText,
    val paymentRef: String
)
