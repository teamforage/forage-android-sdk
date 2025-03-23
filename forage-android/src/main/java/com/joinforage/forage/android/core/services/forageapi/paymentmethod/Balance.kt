package com.joinforage.forage.android.core.services.forageapi.paymentmethod

import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.getStringOrNull
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
    val cash: String,
    val paymentMethodRef: String? = null
) : Balance {
    constructor(jsonObject: JSONObject) : this(
        jsonObject.getString("snap"),
        jsonObject.getString("non_snap")
    )

    internal companion object {
        /** ex: { "snap": "10.00", "cash": "10.00", "paymentMethodRef": "abc123" } */
        internal fun fromSdkResponse(jsonString: String): EbtBalance {
            val jsonObject = JSONObject(jsonString)
            val snap = jsonObject.getString("snap")
            val cash = jsonObject.getString("cash")
            val ref = jsonObject.getStringOrNull("paymentMethodRef")
            return EbtBalance(
                snap = snap,
                cash = cash,
                paymentMethodRef = ref
            )
        }

        /**
         * ex: {
         *   "ref": "e2e5669d77",
         *   "balance": {
         *     "snap": "1000.00",
         *     "non_snap": "1000.00",
         *     "updated": "2024-07-11T07:50:27.355331-07:00"
         *   },
         *   "content_id": "13d711a6-75af-4564-935c-05854f829b5e"
         * }
         */
        internal fun fromVaultResponse(res: ForageApiResponse.Success<String>): EbtBalance {
            val jsonObject = JSONObject(res.data)
            val ref = jsonObject.getStringOrNull("ref")
            val balance = jsonObject.getJSONObject("balance")
            val snap = balance.getString("snap")
            val non_snap = balance.getString("non_snap")
            return EbtBalance(snap = snap, cash = non_snap, paymentMethodRef = ref)
        }
    }

    override fun toString(): String {
        return "{\"snap\":\"${snap}\",\"cash\":\"${cash}\"${paymentMethodRef?.let { ",\"paymentMethodRef\":\"$it\"" } ?: ""}}"
    }

    fun toForageApiResponse(): ForageApiResponse.Success<String> {
        return ForageApiResponse.Success(toString())
    }
}
