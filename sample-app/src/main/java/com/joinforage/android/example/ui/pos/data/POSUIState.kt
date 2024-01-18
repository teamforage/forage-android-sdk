package com.joinforage.android.example.ui.pos.data

import com.joinforage.android.example.ui.pos.MerchantDetailsState

data class POSUIState (
    val merchantId: String = "",
    val cardPAN: String = "",
    val merchantDetailsState: MerchantDetailsState = MerchantDetailsState.Idle,
)