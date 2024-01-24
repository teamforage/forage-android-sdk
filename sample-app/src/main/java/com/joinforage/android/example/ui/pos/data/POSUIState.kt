package com.joinforage.android.example.ui.pos.data

import com.joinforage.android.example.network.model.tokenize.PaymentMethod
import com.joinforage.android.example.ui.pos.MerchantDetailsState
import com.joinforage.forage.android.ui.ForageConfig

data class POSUIState(
    val merchantId: String = "",
    val sessionToken: String = "<your_oath_or_session_token>", // <your_oath_or_session_token>,
    val merchantDetailsState: MerchantDetailsState = MerchantDetailsState.Idle,
    val tokenizedPaymentMethod: PaymentMethod? = null,
    val balance: BalanceCheck? = null
) {
    val forageConfig: ForageConfig
        get() = ForageConfig(merchantId, sessionToken)
}
