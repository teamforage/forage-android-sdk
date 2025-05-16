package com.joinforage.forage.android.core.services.forageapi.paymentmethod

internal class PaymentMethodResponse(val json: String) {
    val parsed = PaymentMethod(json)
}
