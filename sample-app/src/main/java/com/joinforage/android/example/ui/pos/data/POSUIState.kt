package com.joinforage.android.example.ui.pos.data

import com.joinforage.android.example.ui.pos.data.tokenize.PosPaymentMethod
import com.joinforage.forage.android.core.ui.element.ForageConfig

data class POSUIState(
    val merchantId: String = "1234567", // <your_merchant_id>
    val sessionToken: String = "sandbox_eyabcdef...", // <your_oauth_or_session_token>

    // Tokenizing EBT Cards
    val tokenizedPaymentMethod: PosPaymentMethod? = null,
    val tokenizationError: String? = null,

    // Checking balances of those EBT Cards
    val balance: BalanceCheck? = null,
    val balanceCheckError: String? = null,

    // Creating a payment
    val localPayment: PosPaymentRequest? = null, // Used to build up the payment object before we send it
    val createPaymentResponse: PosPaymentResponse? = null,
    val createPaymentError: String? = null,

    // Capturing that payment
    val capturePaymentResponse: PosPaymentResponse? = null,
    val capturePaymentError: String? = null,

    // Refunding a payment
    val localRefundState: RefundUIState? = null, // Used to build up the refund object before we send it
    val refundPaymentResponse: Refund? = null,
    val refundPaymentError: String? = null,

    // Voiding a payment
    val voidPaymentResponse: PosPaymentResponse? = null,
    val voidPaymentError: String? = null,

    // Voiding a refund
    val voidRefundResponse: Refund? = null,
    val voidRefundError: String? = null
) {
    val forageConfig: ForageConfig
        get() = ForageConfig(merchantId, sessionToken)

    val merchant
        get() = Merchant(
            name = "POS Test Merchant",
            ref = "testMerchantRef",
            fns = merchantId,
            address = Address(
                line1 = "171 E 2nd St",
                line2 = null,
                city = "New York",
                state = "NY",
                zipcode = "10009",
                country = "USA"
            )
        )
}

data class RefundUIState(
    val paymentRef: String,
    val amount: Float,
    val reason: String
)
