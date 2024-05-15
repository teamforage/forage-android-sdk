package com.joinforage.forage.android.core.services.forageapi.paymentmethod

import org.json.JSONObject

/**
 * A model that represents the available balance on a customer’s card.
 * @see [EbtBalance]
 */
interface Balance

/**
 * @param snap The available SNAP balance on the customer’s EBT Card, represented as a numeric string.
 * @param cash The available EBT Cash balance on the customer’s EBT Card, represented as a numeric string.
 */
data class EbtBalance(
    val snap: String,
    val cash: String
) : Balance {
    constructor(jsonObject: JSONObject) : this(
        jsonObject.getString("snap"),
        jsonObject.getString("non_snap")
    )

    internal companion object {
        /** ex: { "snap": "10.00", "cash": "10.00" } */
        internal fun fromSdkResponse(jsonString: String): EbtBalance {
            val jsonObject = JSONObject(jsonString)
            val snap = jsonObject.getString("snap")
            val cash = jsonObject.getString("cash")
            return EbtBalance(
                snap = snap,
                cash = cash
            )
        }
    }

    override fun toString(): String {
        return "{\"snap\":\"${snap}\",\"cash\":\"${cash}\"}"
    }
}
