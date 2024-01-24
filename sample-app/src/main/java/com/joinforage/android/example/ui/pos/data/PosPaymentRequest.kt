package com.joinforage.android.example.ui.pos.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PosPaymentRequest(
    val amount: Double,
    @Json(name = "funding_type") val fundingType: String,
    @Json(name = "payment_method") val paymentMethodRef: String,
    val description: String,
    @Json(name = "pos_terminal") val posTerminal: PosTerminal,
    val metadata: Map<String, String>
)

enum class FundingType(val value: String) {
    EBTSnap(value = "ebt_snap"),
    EBTCash(value = "ebt_cash"),
    CreditTPP(value = "credit_tpp")
}

@JsonClass(generateAdapter = true)
data class PosTerminal(
    @Json(name = "provider_terminal_id") val providerTerminalId: String
)
