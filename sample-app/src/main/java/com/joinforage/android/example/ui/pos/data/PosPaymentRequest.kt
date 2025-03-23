package com.joinforage.android.example.ui.pos.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PosPaymentRequest(
    val amount: String,
    @Json(name = "funding_type") val fundingType: String,
    val description: String,
    @Json(name = "pos_terminal") val posTerminal: PosTerminalRequestField,
    val metadata: Map<String, String>,
    @Json(name = "transaction_type") val transactionType: String? = null,
    @Json(name = "cash_back_amount") val cashBackAmount: String? = null
) {
    companion object {
        fun forSnapPayment(snapAmount: String, terminalId: String) = PosPaymentRequest(
            amount = snapAmount,
            description = "Testing POS certification app payments (SNAP Purchase)",
            fundingType = FundingType.EBTSnap.value,
            posTerminal = PosTerminalRequestField(providerTerminalId = terminalId),
            metadata = mapOf()
        )
        fun forEbtCashPayment(ebtCashAmount: String, terminalId: String) = PosPaymentRequest(
            amount = ebtCashAmount,
            description = "Testing POS certification app payments (EBT Cash Purchase)",
            fundingType = FundingType.EBTCash.value,
            posTerminal = PosTerminalRequestField(providerTerminalId = terminalId),
            metadata = mapOf()
        )
        fun forEbtCashWithdrawal(ebtCashWithdrawalAmount: String, terminalId: String) = PosPaymentRequest(
            amount = ebtCashWithdrawalAmount,
            description = "Testing POS certification app payments (EBT Cash Withdrawal)",
            fundingType = FundingType.EBTCash.value,
            posTerminal = PosTerminalRequestField(providerTerminalId = terminalId),
            metadata = mapOf(),
            transactionType = TransactionType.Withdrawal.value
        )
        fun forEbtCashPaymentWithCashBack(ebtCashAmount: String, cashBackAmount: String, terminalId: String) = PosPaymentRequest(
            amount = (ebtCashAmount.toDouble() + cashBackAmount.toDouble()).toString(),
            cashBackAmount = cashBackAmount,
            transactionType = TransactionType.PurchaseWithCashBack.value,
            description = "Testing POS certification app payments (EBT Cash Purchase with Cash Back)",
            fundingType = FundingType.EBTCash.value,
            posTerminal = PosTerminalRequestField(providerTerminalId = terminalId),
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
    Withdrawal(value = "withdrawal"),
    PurchaseWithCashBack(value = "purchase_with_cash_back")
}

@JsonClass(generateAdapter = true)
data class PosTerminalRequestField(
    @Json(name = "provider_terminal_id") val providerTerminalId: String
)
