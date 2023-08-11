package com.joinforage.forage.android.core.element.state

import com.joinforage.forage.android.core.element.ElementValidationError
import com.joinforage.forage.android.core.element.IncompleteEbtPanError
import com.joinforage.forage.android.core.element.InvalidEbtPanError
import com.joinforage.forage.android.core.element.TooLongEbtPanError
import com.joinforage.forage.android.model.hasInvalidStateIIN
import com.joinforage.forage.android.model.isCorrectLength
import com.joinforage.forage.android.model.missingStateIIN
import com.joinforage.forage.android.model.tooLongForStateIIN
import com.joinforage.forage.android.model.tooShortForStateIIN

const val MIN_CARD_LENGTH = 16
const val MAX_CARD_LENGTH = 19

interface PanValidator {
    fun checkIfValid(cardNumber: String): Boolean
    fun checkIfComplete(cardNumber: String): Boolean
    fun checkForValidationError(cardNumber: String): ElementValidationError?
}

class StrictEbtValidator : PanValidator {
    override fun checkIfValid(cardNumber: String): Boolean {
        // we consider a user's input valid if it is too short to
        // contain a StateINN or if it contains a legitimate StateIIN
        // but is not greater than the require length for that StateIIN
        val definitelyInvalid = hasInvalidStateIIN(cardNumber) || tooLongForStateIIN(cardNumber)
        return missingStateIIN(cardNumber) || !definitelyInvalid
    }

    override fun checkIfComplete(cardNumber: String): Boolean {
        return checkIfValid(cardNumber) && isCorrectLength(cardNumber)
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

    fun canTokenizePanElementValue(alwaysAllow: Boolean = false) : Boolean {
        return getState().isComplete || alwaysAllow;
    }

    companion object {
        fun forEmptyInput(): PanElementStateManager {
            return PanElementStateManager(
                INITIAL_ELEMENT_STATE,
                arrayOf(StrictEbtValidator())
            )
        }

        fun NON_PROD_forEmptyInput(): PanElementStateManager {
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
