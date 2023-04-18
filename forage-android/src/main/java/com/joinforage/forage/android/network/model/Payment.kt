package com.joinforage.forage.android.network.model

import org.json.JSONObject

internal data class Payment(
    val ref: String,
    val paymentMethod: String
) {
    object ModelMapper {
        fun from(string: String): Payment {
            val jsonObject = JSONObject(string)

            val ref = jsonObject.getString("ref")
            val paymentMethod = jsonObject.getString("payment_method")

            return Payment(
                ref = ref,
                paymentMethod = paymentMethod
            )
        }
    }
}
