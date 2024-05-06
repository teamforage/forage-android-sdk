package com.joinforage.forage.android.model

/**
 * @property last4 The last 4 digits of the card number
 */
sealed class Card(open val last4: String, internal open val token: String) {
    /**
     * @property last4 The last 4 digits of the EBT card number
     * @property usState The US state that issued the EBT Card
     */
    data class EbtCard(
        override val last4: String,
        override val token: String,
        val usState: USState? = null
    ) : Card(last4 = last4, token = token)
}
