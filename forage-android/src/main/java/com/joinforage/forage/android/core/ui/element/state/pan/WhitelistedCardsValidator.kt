package com.joinforage.forage.android.core.ui.element.state.pan

import com.joinforage.forage.android.core.ui.element.ElementValidationError

internal open class WhitelistedCardsValidator(
    private val prefix: String,
    private val repeatCount: Int = 1
) : PanValidator {
    override fun checkIfValid(cardNumber: String): Boolean {
        // for example 444444 4444 4444 *** **
        // maps to 44444444444444*****, which could be
        // a whitelisted pattern
        return cardNumber.startsWith(prefix.repeat(repeatCount))
    }

    override fun checkIfComplete(cardNumber: String): Boolean {
        return checkIfValid(cardNumber) && cardNumber.length in MIN_CARD_LENGTH..MAX_CARD_LENGTH
    }

    override fun checkForValidationError(cardNumber: String): ElementValidationError? {
        // this is a developer-only validator and should remain
        // quiet by not returning a validationError
        return null
    }
}

internal class PaymentCaptureErrorCard : WhitelistedCardsValidator("4", 14)
internal class BalanceCheckErrorCard : WhitelistedCardsValidator("5", 14)
internal class NonProdValidEbtCard : WhitelistedCardsValidator("9", 4)
internal class EmptyEbtCashBalanceCard : WhitelistedCardsValidator("654321")
internal class PinEncryptionTestCard : WhitelistedCardsValidator("6777")
