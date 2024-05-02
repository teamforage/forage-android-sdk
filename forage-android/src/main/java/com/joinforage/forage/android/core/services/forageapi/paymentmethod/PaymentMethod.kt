package com.joinforage.forage.android.core.services.forageapi.paymentmethod
import com.joinforage.forage.android.core.services.getStringOrNull
import com.joinforage.forage.android.core.services.hasNonNull
import org.json.JSONObject

/**
 * @param card Refer to the [Card] model.
 * @param ref A string identifier that refers to an instance in Forage's database of a PaymentMethod, a tokenized representation of a customer's card.
 * @param type The type of the customer’s payment instrument. ex: "ebt".
 * @param balance Refer to the [Balance] model. `null` until a balance inquiry has been performed.
 * @param customerId A unique identifier for the end customer making the payment.
 * @param reusable Whether the PaymentMethod can be reused. If false, then the PaymentMethod can only be used for a single transaction.
 */
data class PaymentMethod(
    val card: Card,
    val ref: String,
    val type: String,
    val balance: Balance?,
    val customerId: String? = null,
    val reusable: Boolean? = true
) {
    internal constructor(jsonString: String) : this(JSONObject(jsonString))
    internal constructor(jsonObject: JSONObject) : this(
        ref = jsonObject.getString("ref"),
        type = jsonObject.getString("type"),
        customerId = jsonObject.getStringOrNull("customer_id"),
        balance = if (jsonObject.hasNonNull("balance")) {
            EbtBalance(jsonObject.getJSONObject("balance"))
        } else {
            null
        },
        card = EbtCard(jsonObject.getJSONObject("card")),
        reusable = jsonObject.optBoolean("reusable", true)
    )
}