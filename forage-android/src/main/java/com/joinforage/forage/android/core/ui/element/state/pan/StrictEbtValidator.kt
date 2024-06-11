package com.joinforage.forage.android.core.ui.element.state.pan

import com.joinforage.forage.android.core.ui.element.ElementValidationError
import com.joinforage.forage.android.core.ui.element.IncompleteEbtPanError
import com.joinforage.forage.android.core.ui.element.InvalidEbtPanError
import com.joinforage.forage.android.core.ui.element.TooLongEbtPanError

internal class StrictEbtValidator : PanValidator {
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
