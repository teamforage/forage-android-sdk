package com.joinforage.forage.android.model
import com.joinforage.forage.android.getStringOrNull
import org.json.JSONObject

/**
 * @param ref A string identifier that refers to an instance in Forage's database of a PaymentMethod, a tokenized representation of a customer's card.
 * @param type The type of the customer’s payment instrument. ex: "ebt".
 * @param customerId A unique identifier for the end customer making the payment.
 * @param balance Refer to the [Balance] model. `null` until a balance inquiry has been performed.
 * @param card Refer to the [Card] model.
 * @param reusable Whether the PaymentMethod can be reused. If false, then the PaymentMethod can only be used for a single transaction.
 */
data class PaymentMethod(
    val ref: String,
    val type: String,
    val customerId: String? = null,
    val balance: Balance?,
    val card: Card,
    val reusable: Boolean? = true
) {
    internal object ModelMapper {
        internal fun from(string: String): PaymentMethod {
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
                balance = Balance.EbtBalance.ModelMapper.fromApiResponse(parsedBalance)
            }

            val rawCard = jsonObject.getJSONObject("card")
            val last4 = rawCard.getString("last_4")
            val token = rawCard.getString("token")

            var reusable: Boolean? = true
            if (!jsonObject.isNull("reusable")) {
                reusable = jsonObject.getBoolean("reusable")
            }

            val usState = USState.fromAbbreviation(rawCard.getStringOrNull("state"))
            val card = Card.EbtCard(
                last4 = last4,
                token = token,
                usState = usState
            )

            return PaymentMethod(
                ref = ref,
                type = type,
                balance = balance,
                card = card,
                customerId = customerId,
                reusable = reusable
            )
        }
    }
}
