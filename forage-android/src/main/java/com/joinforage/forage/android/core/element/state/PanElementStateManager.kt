package com.joinforage.forage.android.core.element.state

import com.joinforage.forage.android.core.element.ElementValidationError
import com.joinforage.forage.android.core.element.IncompleteEbtPanError
import com.joinforage.forage.android.core.element.InvalidEbtPanError
import com.joinforage.forage.android.core.element.TooLongEbtPanError
import com.joinforage.forage.android.model.STATE_INN_LENGTH
import com.joinforage.forage.android.model.StateIIN

const val MIN_CARD_LENGTH = 16
const val MAX_CARD_LENGTH = 19

private fun missingStateIIN(cardNumber: String): Boolean {
    return cardNumber.length < STATE_INN_LENGTH
}
private fun queryForStateIIN(cardNumber: String): StateIIN? {
    return StateIIN.values().find { cardNumber.startsWith(it.iin) }
}
private fun hasInvalidStateIIN(cardNumber: String): Boolean {
    return queryForStateIIN(cardNumber) == null
}
private fun tooShortForStateIIN(cardNumber: String): Boolean {
    val iin = queryForStateIIN(cardNumber) ?: return false
    return cardNumber.length < iin.panLength
}
private fun tooLongForStateIIN(cardNumber: String): Boolean {
    val iin = queryForStateIIN(cardNumber) ?: return false
    return cardNumber.length > iin.panLength
}
private fun isCorrectLength(cardNumber: String): Boolean {
    return !tooShortForStateIIN(cardNumber) && !tooLongForStateIIN(cardNumber)
}
private fun failsValidation(cardNumber: String): Boolean {
    return missingStateIIN(cardNumber) ||
        hasInvalidStateIIN(cardNumber) ||
        tooLongForStateIIN(cardNumber)
}
private fun passesValidation(cardNumber: String): Boolean {
    return !failsValidation(cardNumber)
}

interface PanValidator {
    fun checkIfValid(cardNumber: String): Boolean
    fun checkIfComplete(cardNumber: String): Boolean
    fun checkForValidationError(cardNumber: String): ElementValidationError?
}

class StrictEbtValidator : PanValidator {
    override fun checkIfValid(cardNumber: String): Boolean {
        return cardNumber.isEmpty() || passesValidation(cardNumber)
    }

    override fun checkIfComplete(cardNumber: String): Boolean {
        return passesValidation(cardNumber) && isCorrectLength(cardNumber)
    }

    override fun checkForValidationError(cardNumber: String): ElementValidationError? {
        return if (cardNumber.isEmpty()) {
            null
        } else if (missingStateIIN(cardNumber)) {
            IncompleteEbtPanError
        } else if (hasInvalidStateIIN(cardNumber)) {
            InvalidEbtPanError
        } else if (tooShortForStateIIN(cardNumber)) {
            IncompleteEbtPanError
        } else if (tooLongForStateIIN(cardNumber)) {
            TooLongEbtPanError
        } else {
            null
        }
    }
}

open class WhitelistedCards(
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

class PaymentCaptureErrorCard : WhitelistedCards("4", 14)
class BalanceCheckErrorCard : WhitelistedCards("5", 14)
class NonProdValidEbtCard : WhitelistedCards("9", 4)
class EmptyEbtCashBalanceCard : WhitelistedCards("654321")

class PanElementStateManager(state: ElementState, private val validators: Array<PanValidator>) : ElementStateManager(state) {

    private fun checkIsValid(cardNumber: String): Boolean {
        return validators.any { it.checkIfValid(cardNumber) }
    }
    private fun checkIfComplete(cardNumber: String): Boolean {
        return validators.any { it.checkIfComplete(cardNumber) }
    }
    private fun checkForValidationError(cardNumber: String): ElementValidationError? {
        return validators
            .map { it.checkForValidationError(cardNumber) }
            .firstOrNull { it != null }
    }

    fun handleChangeEvent(rawInput: String) {
        // because the input may be formatted, we need to
        // strip everything but digits
        val newCardNumber = rawInput.filter { it.isDigit() }

        // check to see if any of the validators believe the
        // card to be valid
        this.isValid = checkIsValid(newCardNumber)
        this.isComplete = checkIfComplete(newCardNumber)
        this.validationError = checkForValidationError(newCardNumber)
        this.isEmpty = newCardNumber.isEmpty()

        // invoke the registered listener with the updated state
        onChangeEventListener?.invoke(getState())
    }

    companion object {
        fun forEmptyInput(): PanElementStateManager {
            return PanElementStateManager(
                INITIAL_ELEMENT_STATE,
                arrayOf(StrictEbtValidator())
            )
        }

        fun DEV_ONLY_forEmptyInput(): PanElementStateManager {
            return PanElementStateManager(
                INITIAL_ELEMENT_STATE,
                arrayOf(
                    StrictEbtValidator(),
                    PaymentCaptureErrorCard(),
                    BalanceCheckErrorCard(),
                    NonProdValidEbtCard(),
                    EmptyEbtCashBalanceCard()
                )
            )
        }
    }
}
