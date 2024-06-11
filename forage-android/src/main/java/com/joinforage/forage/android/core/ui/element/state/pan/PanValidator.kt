package com.joinforage.forage.android.core.ui.element.state.pan

import com.joinforage.forage.android.core.ui.element.ElementValidationError

internal const val MIN_CARD_LENGTH = 16
internal const val MAX_CARD_LENGTH = 19

internal interface PanValidator {
    fun checkIfValid(cardNumber: String): Boolean
    fun checkIfComplete(cardNumber: String): Boolean
    fun checkForValidationError(cardNumber: String): ElementValidationError?
}
