package com.joinforage.android.example.ui.pos.data

import com.joinforage.android.example.network.model.PaymentResponse
import com.joinforage.android.example.network.model.tokenize.PaymentMethod
import com.joinforage.android.example.ui.pos.MerchantDetailsState
import com.joinforage.forage.android.ui.ForageConfig

data class POSUIState(
    val merchantId: String = "<your_merchant_id>", // <your_merchant_id>
    val sessionToken: String = "<your_oath_or_session_token>", // <your_oath_or_session_token>
    val merchantDetailsState: MerchantDetailsState = MerchantDetailsState.Idle,

    // Tokenizing EBT Cards
    val tokenizedPaymentMethod: PaymentMethod? = null,
    val tokenizationError: String? = null,

    // Checking balances of those EBT Cards
    val balance: BalanceCheck? = null,
    val balanceCheckError: String? = null,

    // Creating a payment
    val localPayment: PosPaymentRequest? = null, // Used to build up the payment object before we send it
    val createPaymentResponse: PaymentResponse? = null,
    val createPaymentError: String? = null,

    // Capturing that payment
    val capturePaymentResponse: PaymentResponse? = null,
    val capturePaymentError: String? = null,

    // Voiding a payment
    val voidPaymentResponse: PaymentResponse? = null,
    val voidPaymentError: String? = null,

    // Voiding a refund
    val voidRefundResponse: PaymentResponse? = null,
    val voidRefundError: String? = null
) {
    val forageConfig: ForageConfig
        get() = ForageConfig(merchantId, sessionToken)
}
