package com.joinforage.forage.android.core.element.state

import com.joinforage.forage.android.core.element.IncompleteEbtPanError

class PanElementStateManager(state: ElementState) : ElementStateManager(state) {

    private fun setIsValid(cardNumber: String) {
        // TODO: update the logic to support PAN validation based on
        //  state IINs. That can come later though
        if (cardNumber.isEmpty()) {
            isValid = true
            validationError = null
        } else if (cardNumber.length < 16) {
            isValid = false
            validationError = IncompleteEbtPanError
        } else {
            isValid = true
            validationError = null
        }
    }

    private fun setIsComplete(cardNumber: String) {
        isComplete = cardNumber.length >= 16
    }

    private fun setIsEmpty(cardNumber: String) {
        this.isEmpty = cardNumber.isEmpty()
    }

    fun handleChangeEvent(newCardNumber: String) {
        setIsValid(newCardNumber)
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
