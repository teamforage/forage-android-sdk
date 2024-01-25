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
    val metadata: Map<String, String>,
    @Json(name = "transaction_type") val transactionType: String? = null,
    @Json(name = "cash_back_amount") val cashBackAmount: Double? = null
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
        fun forEbtCashPaymentWithCashBack(ebtCashAmount: Double, cashBackAmount: Double, terminalId: String) = PosPaymentRequest(
            amount = ebtCashAmount + cashBackAmount,
            cashBackAmount = cashBackAmount,
            transactionType = TransactionType.PurchaseWithCashBack.value,
            description = "Testing POS certification app payments (EBT Cash Purchase with Cash Back)",
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

enum class TransactionType(val value: String) {
    PurchaseWithCashBack(value = "purchase_with_cash_back")
}

@JsonClass(generateAdapter = true)
data class PosTerminal(
    @Json(name = "provider_terminal_id") val providerTerminalId: String
)
