package com.joinforage.forage.android.network.model

import org.json.JSONObject

internal data class PaymentMethodRequestBody(
    val cardNumber: String,
    val type: String = "ebt",
    val reusable: Boolean = true
)

internal fun PaymentMethodRequestBody.toJSONObject(): JSONObject {
    val cardObject = JSONObject()
    cardObject.put("number", cardNumber)

    val rootObject = JSONObject()

    rootObject.put("card", cardObject)
    rootObject.put("type", type)
    rootObject.put("reusable", reusable)

    return rootObject
}
