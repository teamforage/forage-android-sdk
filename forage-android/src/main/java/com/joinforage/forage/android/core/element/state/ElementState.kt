package com.joinforage.forage.android.core.element.state

import com.joinforage.forage.android.core.element.ElementValidationError
import com.joinforage.forage.android.model.USState

interface ElementState {
    val isFocused: Boolean
    val isBlurred: Boolean
    val isEmpty: Boolean
    val isValid: Boolean
    val isComplete: Boolean
    val validationError: ElementValidationError?
}

interface PinElementState : ElementState

internal data class PinElementStateDto(
    override val isFocused: Boolean,
    override val isBlurred: Boolean,
    override val isEmpty: Boolean,
    override val isValid: Boolean,
    override val isComplete: Boolean,
    override val validationError: ElementValidationError?
) : PinElementState

internal val INITIAL_PIN_ELEMENT_STATE = PinElementStateDto(
    isFocused = false,
    isBlurred = true,
    isEmpty = true,
    isValid = false,
    isComplete = false,
    validationError = null
)

interface DerivedCardInfo {
    val usState: USState?
}

internal data class DerivedCardInfoDto(
    override val usState: USState? = null
) : DerivedCardInfo

interface PanElementState : ElementState {
    val derivedCardInfo: DerivedCardInfo // the interface not the DTO
}

internal data class PanElementStateDto(
    override val isFocused: Boolean,
    override val isBlurred: Boolean,
    override val isEmpty: Boolean,
    override val isValid: Boolean,
    override val isComplete: Boolean,
    override val validationError: ElementValidationError?,
    override val derivedCardInfo: DerivedCardInfoDto
) : PanElementState

internal val INITIAL_PAN_ELEMENT_STATE = PanElementStateDto(
    isFocused = false,
    isBlurred = true,
    isEmpty = true,
    isValid = true,
    isComplete = false,
    validationError = null,
    derivedCardInfo = DerivedCardInfoDto()
)
