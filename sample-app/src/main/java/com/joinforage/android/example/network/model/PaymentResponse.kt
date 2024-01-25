package com.joinforage.android.example.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PaymentResponse(
    @Json(name = "ref")
    var ref: String?,
    @Json(name = "merchant")
    var merchant: String?,
    @Json(name = "funding_type")
    var fundingType: String?,
    var amount: String?,
    var description: String?,
    var metadata: Map<String, String>?,
    @Json(name = "payment_method")
    var paymentMethod: String?,
    @Json(name = "delivery_address")
    var deliveryAddress: Address?,
    @Json(name = "is_delivery")
    var isDelivery: Boolean?,
    var created: String?,
    var updated: String?,
    var status: String?,
    @Json(name = "last_processing_error")
    var lastProcessingError: String?,
    @Json(name = "success_date")
    var successDate: String?,
    var refunds: List<String>,
    @Json(name = "pos_terminal")
    var posTerminal: PosTerminal,
    @Json(name = "customer_id")
    var customerId: String?
) {
    val forageTerminalId
        get() = posTerminal.terminalId
    val deviceTerminalId
        get() = posTerminal.providerTerminalId
}

@JsonClass(generateAdapter = true)
data class PosTerminal(
    @Json(name = "terminal_id")
    var terminalId: String?,
    @Json(name = "provider_terminal_id")
    var providerTerminalId: String?
)
