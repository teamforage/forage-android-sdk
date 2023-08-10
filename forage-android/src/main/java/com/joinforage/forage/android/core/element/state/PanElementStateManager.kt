package com.joinforage.forage.android.core.element.state

import com.joinforage.forage.android.core.element.IncompleteEbtPanError
import com.joinforage.forage.android.core.element.InvalidEbtPanError
import com.joinforage.forage.android.core.element.TooLongEbtPanError
import com.joinforage.forage.android.model.STATE_INN_LENGTH
import com.joinforage.forage.android.model.StateIIN

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
private fun failsValidation(cardNumber: String): Boolean {
    return missingStateIIN(cardNumber) ||
        hasInvalidStateIIN(cardNumber) ||
        tooShortForStateIIN(cardNumber) ||
        tooLongForStateIIN(cardNumber)
}
private fun passesValidation(cardNumber: String): Boolean {
    return !failsValidation(cardNumber)
}

class PanElementStateManager(state: ElementState) : ElementStateManager(state) {

    private fun setIsValid(cardNumber: String) {
        isValid = cardNumber.isEmpty() || passesValidation(cardNumber)
    }

    private fun setValidationError(cardNumber: String) {
        validationError = if (cardNumber.isEmpty()) {
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
        isComplete = cardNumber.isNotEmpty() && passesValidation(cardNumber)
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
