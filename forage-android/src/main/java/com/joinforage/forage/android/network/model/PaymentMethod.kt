package com.joinforage.forage.android.network.model

import com.joinforage.forage.android.model.Balance
import com.joinforage.forage.android.model.Card
import org.json.JSONObject

data class PaymentMethod(
    val ref: String,
    val type: String,
    val balance: Balance?,
    val card: Card?
) {
    object ModelMapper {
        fun from(string: String): PaymentMethod {
            val jsonObject = JSONObject(string)

            val ref = jsonObject.getString("ref")
            val type = jsonObject.getString("type")

            val card = jsonObject.getJSONObject("card")
            val last4 = card.getString("last_4")
            val token = card.getString("token")

            return PaymentMethod(
                ref = ref,
                type = type,
                balance = null,
                card = Card(
                    last4 = last4,
                    type = "",
                    token = token
                )
            )
        }
    }
}
