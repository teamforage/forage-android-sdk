package com.joinforage.forage.android.model
import org.json.JSONObject

internal data class Card(
    val last4: String,
    val type: String = "",
    val token: String,
    // POS-only
    val number: String? = null
)

internal data class Balance(
    val snap: String,
    val cash: String
) {
    object ModelMapper {
        fun from(string: String): Balance {
            val jsonObject = JSONObject(string)

            val snap = jsonObject.getString("snap")
            val cash = jsonObject.getString("non_snap")

            return Balance(
                snap = snap,
                cash = cash
            )
        }
    }

    override fun toString(): String {
        return "{\"snap\":\"${snap}\",\"cash\":\"${cash}\"}"
    }
}

internal data class PaymentMethod(
    val ref: String,
    val type: String,
    val customerId: String? = null,
    val balance: Balance?,
    val card: Card,
    val reusable: Boolean? = true
) {
    object ModelMapper {
        fun from(string: String): PaymentMethod {
            val jsonObject = JSONObject(string)

            val ref = jsonObject.getString("ref")
            val type = jsonObject.getString("type")
            val customerId = if (jsonObject.has("customer_id")) {
                jsonObject.getString("customer_id")
            } else {
                null
            }
            var balance: Balance? = null
            if (!jsonObject.isNull("balance")) {
                val parsedBalance = jsonObject.getJSONObject("balance")
                val snap = parsedBalance.getString("snap")
                val cash = parsedBalance.getString("non_snap")
                balance = Balance(
                    snap = snap,
                    cash = cash
                )
            }

            val card = jsonObject.getJSONObject("card")
            val last4 = card.getString("last_4")
            val token = card.getString("token")

            val cardNumber = if (!card.isNull("number")) card.getString("number") else null

            var reusable: Boolean? = true
            if (!jsonObject.isNull("reusable")) {
                reusable = jsonObject.getBoolean("reusable")
            }

            return PaymentMethod(
                ref = ref,
                type = type,
                balance = balance,
                card = Card(
                    last4 = last4,
                    type = "",
                    number = cardNumber,
                    token = token
                ),
                customerId = customerId,
                reusable = reusable
            )
        }
    }
}
