package com.joinforage.android.example.ui.pos.data

import javax.annotation.Nullable

data class POSUIState (
    val merchantId: String = "",
    val cardPAN: String = "",
    val merchantInfo: Merchant? = null,
)