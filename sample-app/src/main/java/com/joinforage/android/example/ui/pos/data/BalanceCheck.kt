package com.joinforage.android.example.ui.pos.data

import com.squareup.moshi.JsonClass

// TODO: Devin to circle back with Tiffany when she adds the TerminalId value
//  to the BalanceCheck response...we need that to show receipts for balance check
@JsonClass(generateAdapter = true)
data class BalanceCheck(
    val snap: String,
    val cash: String
)
