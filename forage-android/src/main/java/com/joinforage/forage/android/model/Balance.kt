package com.joinforage.forage.android.model

import kotlinx.parcelize.Parcelize
import org.json.JSONObject

@Parcelize
sealed class Balance : ForageModel {
    /**
     * @param snap The available SNAP balance on the customer’s EBT Card, represented as a numeric string.
     * @param cash The available EBT Cash balance on the customer’s EBT Card, represented as a numeric string.
     */
    @Parcelize
    data class EbtBalance(
        val snap: String,
        val cash: String
    ) : Balance() {
        internal object ModelMapper {
            /** ex: { "snap": "10.00", "non_snap": "10.00" } */
            internal fun fromApiResponse(jsonObject: JSONObject): EbtBalance {
                val snap = jsonObject.getString("snap")
                val cash = jsonObject.getString("non_snap")

                return EbtBalance(
                    snap = snap,
                    cash = cash
                )
            }

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
}


