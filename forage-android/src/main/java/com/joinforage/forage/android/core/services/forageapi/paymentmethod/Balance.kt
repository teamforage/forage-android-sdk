package com.joinforage.forage.android.core.services.forageapi.paymentmethod

import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
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

        /**
         * ex: {
         *   "ref": "e2e5669d77",
         *   "balance": {
         *     "snap": "1000.00",
         *     "non_snap": "1000.00",
         *     "updated": "2024-07-11T07:50:27.355331-07:00"
         *   }
         * }
         */
        internal fun fromVaultResponse(res: ForageApiResponse.Success<String>): EbtBalance {
            val jsonObject = JSONObject(res.data)
            val balance = jsonObject.getJSONObject("balance")
            return EbtBalance(balance)
        }
    }

    override fun toString(): String {
        return "{\"snap\":\"${snap}\",\"cash\":\"${cash}\"}"
    }

    fun toForageApiResponse(): ForageApiResponse.Success<String> {
        return ForageApiResponse.Success(toString())
    }
}
