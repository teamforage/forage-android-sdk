package com.joinforage.forage.android.core.services.forageapi.paymentmethod

/**
 * @property last4 The last 4 digits of the card number.
 * @property brand
 * @property expMonth
 * @property expYear
 * @property isHsaFsa
 */
interface CreditDebitCard : Card {
    override val last4: String
    val brand: String
    val expMonth: Int
    val expYear: Int
    val isHsaFsa: Boolean
}
