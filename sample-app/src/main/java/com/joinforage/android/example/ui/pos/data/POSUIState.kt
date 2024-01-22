package com.joinforage.android.example.ui.pos.data

import com.joinforage.android.example.network.model.tokenize.PaymentMethod
import com.joinforage.android.example.ui.pos.MerchantDetailsState

data class POSUIState(
    val merchantId: String = "",
    val merchantDetailsState: MerchantDetailsState = MerchantDetailsState.Idle,
    val tokenizedPaymentMethod: PaymentMethod? = null
)
