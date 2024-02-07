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
