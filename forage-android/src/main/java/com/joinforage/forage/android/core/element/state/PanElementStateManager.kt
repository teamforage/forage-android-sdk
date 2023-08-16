package com.joinforage.forage.android.core.element.state

import com.joinforage.forage.android.BuildConfig
import com.joinforage.forage.android.core.element.IncompleteEbtPanError
import com.joinforage.forage.android.core.element.InvalidEbtPanError
import com.joinforage.forage.android.core.element.TooLongEbtPanError
import com.joinforage.forage.android.model.STATE_INN_LENGTH
import com.joinforage.forage.android.model.StateIIN

const val PROD = "prod"

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

class PanElementStateManager(state: ElementState) : ElementStateManager(state) {
    private val errorCardPaymentCapture = Regex("^4{14}.*")
    private val errorCardBalanceCheck = Regex("^5{14}.*")
    private val nonProdValidEbtCards = Regex("^9{4}.*")

    private fun overrideNonProdCheck(cardNumber: String): Boolean {
        return cardNumber.matches(errorCardPaymentCapture) ||
            cardNumber.matches(errorCardBalanceCheck) ||
            cardNumber.matches(nonProdValidEbtCards)
    }

    private fun setIsValid(cardNumber: String) {
        var overrideValidCheck = false
        if (BuildConfig.FLAVOR != PROD) {
            overrideValidCheck = overrideNonProdCheck(cardNumber) || cardNumber.length < 6
        }
        isValid = cardNumber.isEmpty() || passesValidation(cardNumber) || overrideValidCheck
    }

    private fun setValidationError(cardNumber: String) {
        validationError = if (cardNumber.isEmpty()) {
            null
        } else if (BuildConfig.FLAVOR != PROD && overrideNonProdCheck(cardNumber)) {
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

    private fun setIsComplete(cardNumber: String) {
        isComplete = if (BuildConfig.FLAVOR != PROD && overrideNonProdCheck(cardNumber)) {
            cardNumber.length in 16..19
        } else {
            passesValidation(cardNumber) &&
                isCorrectLength(cardNumber)
        }
    }

    private fun setIsEmpty(cardNumber: String) {
        this.isEmpty = cardNumber.isEmpty()
    }

    fun handleChangeEvent(newCardNumber: String) {
        setIsValid(newCardNumber)
        setValidationError(newCardNumber)
        setIsComplete(newCardNumber)
        setIsEmpty(newCardNumber)
        onChangeEventListener?.invoke(getState())
    }

    companion object {
        fun forEmptyInput(): PanElementStateManager {
            return PanElementStateManager(INITIAL_ELEMENT_STATE)
        }
    }
}
