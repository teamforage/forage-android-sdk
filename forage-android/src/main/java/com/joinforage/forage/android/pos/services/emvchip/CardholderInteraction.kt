package com.joinforage.forage.android.pos.services.emvchip

/**
 * Represents the information captured by a point-of-sale terminal's card reader during a transaction.
 * This interface is used to handle and transfer card interaction data within the SDK.
 *
 * @property rawPan The Primary Account Number (PAN) of the card, representing the card number.
 * @property track2Data Information from the second track of the card's magnetic stripe, which typically contains the PAN, expiration date, service code, and discretionary data.
 * @property type An instance of `CardholderInteractionType`, indicating the method of card entry, such as swipe, tap, insert, or manual entry.
 */
interface CardholderInteraction {
    val rawPan: String
    val type: CardholderInteractionType
    val track2Data: String?
}
