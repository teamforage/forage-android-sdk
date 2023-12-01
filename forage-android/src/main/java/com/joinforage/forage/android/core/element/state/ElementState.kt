package com.joinforage.forage.android.core.element.state

import com.joinforage.forage.android.core.element.ElementValidationError
import com.joinforage.forage.android.model.USState

data class ElementState<InputDetails>(
    val isFocused: Boolean,
    val isBlurred: Boolean,
    val isEmpty: Boolean,
    val isValid: Boolean,
    val isComplete: Boolean,
    val validationError: ElementValidationError?,
    val details: InputDetails
)

internal typealias PinDetails = Nothing?
internal val INITIAL_PIN_ELEMENT_STATE = ElementState<PinDetails>(
    isFocused = false,
    isBlurred = true,
    isEmpty = true,
    isValid = true,
    isComplete = false,
    validationError = null,
    details = null
)

data class DerivedCardInfo(val usState: USState? = null)
data class PanDetails(
    val derivedCardInfo: DerivedCardInfo
)
internal val INITIAL_PAN_ELEMENT_STATE = ElementState<PanDetails>(
    isFocused = false,
    isBlurred = true,
    isEmpty = true,
    isValid = true,
    isComplete = false,
    validationError = null,
    details = PanDetails(DerivedCardInfo())
)
