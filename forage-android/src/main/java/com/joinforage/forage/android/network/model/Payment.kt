package com.joinforage.forage.android.network.model

import com.joinforage.forage.android.model.Balance
import com.joinforage.forage.android.model.Card
import org.json.JSONObject

internal data class Payment(
    val ref: String,
    val paymentMethod: String,
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
