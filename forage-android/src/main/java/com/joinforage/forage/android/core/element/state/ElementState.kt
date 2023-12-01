package com.joinforage.forage.android.core.element.state

import com.joinforage.forage.android.core.element.ElementValidationError
import com.joinforage.forage.android.ui.DerivedCardInfo

data class ElementState(
    val isFocused: Boolean,
    val isBlurred: Boolean,
    val isEmpty: Boolean,
    val isValid: Boolean,
    val isComplete: Boolean,
    val validationError: ElementValidationError?,
    val derivedCardInfo: DerivedCardInfo
)
internal val INITIAL_ELEMENT_STATE = ElementState(
    isFocused = false,
    isBlurred = true,
    isEmpty = true,
    isValid = true,
    isComplete = false,
    validationError = null,
    derivedCardInfo = DerivedCardInfo()
)
