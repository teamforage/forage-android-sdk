package com.joinforage.forage.android.core.element.state

import com.joinforage.forage.android.core.element.ElementValidationError
import com.joinforage.forage.android.model.USState

/**
 * An interface that represents the state of the [ForageElement][com.joinforage.forage.android.ui.ForageElement]
 * as the customer interacts with it.
 * @property isFocused Whether the Element is in focus.
 * @property isBlurred Whether the Element is blurred.
 * @property isEmpty Whether the input value of the Element is empty.
 * @property isValid Whether the input text fails any validation checks, with the exception of the
 * target length constraint.
 * @property isComplete Whether the text field of the Element is ready to submit. This is `true`
 * when all input value validation constraints are satisfied.
 * @property validationError An [ElementValidationError], if `isValid` is `false`.
 */
interface ElementState {
    val isFocused: Boolean
    val isBlurred: Boolean
    val isEmpty: Boolean
    val isValid: Boolean
    val isComplete: Boolean
    val validationError: ElementValidationError?
}

/**
 * An interface that represents the state of a
 * [ForagePINEditText][com.joinforage.forage.android.ui.ForagePINEditText] Element.
 */
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

/**
 * An interface that represents information that Forage gets from the card.
 * This includes the [USState] that issued the card.
 */
interface DerivedCardInfo {
    val usState: USState?
}

internal data class DerivedCardInfoDto(
    override val usState: USState? = null
) : DerivedCardInfo

/**
 * An interface that represents the state of a
 * [ForagePANEditText][com.joinforage.forage.android.ui.ForagePANEditText] Element.
 * @property derivedCardInfo The [derivedCardInfo] for the card number submitted to the Element.
 */
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
