package com.joinforage.android.example.ui.pos.data

import com.joinforage.android.example.ui.pos.data.tokenize.PosTerminalResponseField
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PosPaymentResponse(
    @Json(name = "ref")
    var ref: String?,
    @Json(name = "merchant")
    var merchant: String?,
    @Json(name = "funding_type")
    var fundingType: String?,
    var amount: Float?,
    var description: String?,
    var metadata: Map<String, String>?,
    @Json(name = "payment_method")
    var paymentMethod: String,
    @Json(name = "delivery_address")
    var deliveryAddress: Address?,
    @Json(name = "is_delivery")
    var isDelivery: Boolean?,
    var created: String,
    var updated: String?,
    var status: String?,
    @Json(name = "last_processing_error")
    var lastProcessingError: String?,
    @Json(name = "success_date")
    var successDate: String?,
    var receipt: Receipt?,
    var refunds: List<String>,
    @Json(name = "pos_terminal")
    val posTerminal: PosTerminalResponseField?,
    @Json(name = "customer_id")
    var customerId: String?,
    @Json(name = "cash_back_amount")
    var cashBackAmount: Float?,
    @Json(name = "sequence_number")
    var sequenceNumber: String?,
    @Json(name = "transaction_type")
    var transactionType: String?
)
