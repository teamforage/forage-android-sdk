package com.joinforage.android.example.ui.pos.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Refund(
    val ref: String,
    @Json(name = "payment_ref") val paymentRef: String,
    @Json(name = "funding_type") val fundingType: String,
    val amount: String,
    val reason: String,
    val metadata: Map<String, String>,
    val created: String,
    val updated: String,
    val status: String,
    @Json(name = "last_processing_error") val lastProcessingError: String?,
    val receipt: Receipt,
    @Json(name = "pos_terminal") val posTerminal: RefundPosTerminal,
    @Json(name = "external_order_id") val externalOrderId: String?,
    val messages: RefundVoidMessages?
)

@JsonClass(generateAdapter = true)
data class Receipt(
    @Json(name = "ref_number") val refNumber: String,
    @Json(name = "is_voided") val isVoided: Boolean,
    @Json(name = "snap_amount") val snapAmount: String,
    @Json(name = "ebt_cash_amount") val ebtCashAmount: String,
    @Json(name = "cash_back_amount") val cashBackAmount: String,
    @Json(name = "other_amount") val otherAmount: String,
    @Json(name = "sales_tax_applied") val salesTaxApplied: String,
    val balance: ReceiptBalance,
    @Json(name = "last_4") val last4: String,
    val message: String,
    @Json(name = "transaction_type") val transactionType: String,
    val created: String,
    @Json(name = "sequence_number") val sequenceNumber: String
)

@JsonClass(generateAdapter = true)
data class ReceiptBalance(
    val id: Double,
    val snap: String,
    @Json(name = "non_snap") val nonSnap: String,
    val updated: String
)

@JsonClass(generateAdapter = true)
data class RefundPosTerminal(
    @Json(name = "terminal_id") val terminalId: String,
    @Json(name = "provider_terminal_id") val providerTerminalId: String
)

@JsonClass(generateAdapter = true)
data class RefundVoidMessages(
    @Json(name = "content_id") val contentId: String,
    @Json(name = "message_type") val messageType: String,
    val status: String,
    val failed: Boolean,
    val errors: Array<String>
)
