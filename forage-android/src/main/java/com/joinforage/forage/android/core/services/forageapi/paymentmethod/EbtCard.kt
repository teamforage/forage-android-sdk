package com.joinforage.forage.android.core.services.forageapi.paymentmethod

import com.joinforage.forage.android.core.services.getStringOrNull
import com.joinforage.forage.android.core.ui.element.state.pan.USState
import org.json.JSONObject

/**
 * @property last4 The last 4 digits of the EBT Card number.
 * @property usState The US state that issued the EBT Card.
 * @property token A unique hash based on the card PAN. The fingerprint is constant for a card PAN,
 * no matter the `customerId`. Use the [fingerprint] to track card usage details for fraud prevention.
 */
data class EbtCard(
    override val last4: String,
    val fingerprint: String,
    internal val token: String,
    internal val number: String?, // POS only; used internally
    val usState: USState? = null
) : Card {
    internal constructor(jsonObject: JSONObject) : this(
        last4 = jsonObject.getString("last_4"),
        token = jsonObject.getString("token"),
        fingerprint = jsonObject.getString("fingerprint"),
        number = jsonObject.getStringOrNull("number"),
        usState = USState.fromAbbreviation(jsonObject.getStringOrNull("state"))
    )

    class MissingFullPanException(val paymentMethodRef: String) : Exception()
}
