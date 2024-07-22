package com.joinforage.forage.android.core.services.forageapi.network.error

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
    data class EbtError51Details(
        val snapBalance: String?,
        val cashBalance: String?
    ) : ForageErrorDetails() {
        internal constructor(detailsJson: JSONObject?) : this(
            detailsJson?.opt("snap_balance") as String?,
            detailsJson?.opt("cash_balance") as String?
        )

        /**
         * A method that converts the [EbtError51Details] to a string.
         *
         * @return A string representation of [EbtError51Details].
         */
        override fun toString(): String = "Cash Balance: $cashBalance\nSNAP Balance: $snapBalance"
    }

    companion object {
        fun from(forageCode: String, jsonForageError: JSONObject?): ForageErrorDetails? {
            val jsonDetails = jsonForageError?.optJSONObject("details") ?: return null
            return when (forageCode) {
                "ebt_error_51" -> EbtError51Details(jsonDetails)
                else -> null
            }
        }
    }
}
