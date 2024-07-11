package com.joinforage.forage.android.core.services.forageapi.network

import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.network.ForageError
import org.json.JSONObject

/**
 * A model that is a blueprint for all possible types of `details` returned in a [ForageError]
 * response.
 */
sealed class ForageErrorDetails {
    /**
     * An error that is returned when a customer's EBT Card balance is insufficient to complete a
     * payment.
     * @property snapBalance A string that represents the available SNAP balance on the EBT Card.
     * @property cashBalance A string that represents the available EBT Cash balance on the EBT Card.
     * @see [Forage guide to handling insufficient funds](https://docs.joinforage.app/docs/plan-for-insufficient-ebt-funds-errors)
     */
    data class EbtError51Details(val snapBalance: String? = null, val cashBalance: String? = null) : ForageErrorDetails() {
        internal companion object {
            fun from(detailsJson: JSONObject?): EbtError51Details {
                // TODO: should probably add a log here if detailsJSON
                //  is null since it should not be null if this is called
                val snapBalance = detailsJson?.optString("snap_balance", null)
                val cashBalance = detailsJson?.optString("cash_balance", null)
                return EbtError51Details(snapBalance, cashBalance)
            }
        }

        /**
         * A method that converts the [EbtError51Details] to a string.
         *
         * @return A string representation of [EbtError51Details].
         */
        override fun toString(): String = "Cash Balance: $cashBalance\nSNAP Balance: $snapBalance"
    }
}