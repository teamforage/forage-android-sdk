package com.joinforage.forage.android.network.model

import org.json.JSONObject

internal data class PaymentMethodRequestBody(
    val cardNumber: String,
    val type: String = "ebt",
    val reusable: Boolean = true,
    val userId: String? = null
)

internal fun PaymentMethodRequestBody.toJSONObject(): JSONObject {
    val cardObject = JSONObject()
    cardObject.put("number", cardNumber)

    val rootObject = JSONObject()

    rootObject.put("card", cardObject)
    rootObject.put("type", type)
    rootObject.put("reusable", reusable)
    if (userId != null) {
        rootObject.put("user_id", userId)
    }

    return rootObject
}
