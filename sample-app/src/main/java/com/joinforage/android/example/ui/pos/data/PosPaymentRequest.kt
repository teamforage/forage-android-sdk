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
) {
    companion object {
        fun forSnapPayment(snapAmount: Double, terminalId: String) = PosPaymentRequest(
            amount = snapAmount,
            description = "Testing POS certification app payments (SNAP Purchase)",
            fundingType = FundingType.EBTSnap.value,
            paymentMethodRef = "",
            posTerminal = PosTerminal(providerTerminalId = terminalId),
            metadata = mapOf()
        )
        fun forEbtCashPayment(ebtCashAmount: Double, terminalId: String) = PosPaymentRequest(
            amount = ebtCashAmount,
            description = "Testing POS certification app payments (EBT Cash Purchase)",
            fundingType = FundingType.EBTCash.value,
            paymentMethodRef = "",
            posTerminal = PosTerminal(providerTerminalId = terminalId),
            metadata = mapOf()
        )
    }
}

enum class FundingType(val value: String) {
    EBTSnap(value = "ebt_snap"),
    EBTCash(value = "ebt_cash"),
    CreditTPP(value = "credit_tpp")
}

@JsonClass(generateAdapter = true)
data class PosTerminal(
    @Json(name = "provider_terminal_id") val providerTerminalId: String
)
