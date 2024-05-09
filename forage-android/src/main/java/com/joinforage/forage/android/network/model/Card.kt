package com.joinforage.forage.android.network.model

import com.joinforage.forage.android.getStringOrNull
import com.joinforage.forage.android.model.USState
import org.json.JSONObject

/**
 * @property last4 The last 4 digits of the card number.
 */
interface Card {
    val last4: String
}

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
    val usState: USState? = null
) : Card {
    internal constructor(jsonObject: JSONObject) : this(
        last4 = jsonObject.getString("last_4"),
        token = jsonObject.getString("token"),
        fingerprint = jsonObject.getString("fingerprint"),
        usState = USState.fromAbbreviation(jsonObject.getStringOrNull("state"))
    )
}
