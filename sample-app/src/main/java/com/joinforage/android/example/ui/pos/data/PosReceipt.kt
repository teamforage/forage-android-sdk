package com.joinforage.android.example.ui.pos.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Receipt(
    @Json(name = "ref_number") val refNumber: String,
    @Json(name = "is_voided") var isVoided: Boolean,
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
    var snap: String,
    @Json(name = "non_snap") var nonSnap: String,
    val updated: String
)
