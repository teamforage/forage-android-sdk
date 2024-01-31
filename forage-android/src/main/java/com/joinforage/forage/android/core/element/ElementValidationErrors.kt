package com.joinforage.forage.android.core.element

/**
 * A model that represents an error related to an incomplete or incorrect customer input.
 * @property detail A message that describes the error.
 */
data class ElementValidationError(
    val detail: String
)

// PAN Input Errors
/**
 * A type of [ElementValidationError] that is thrown when a customer submits an incomplete
 * EBT Card number.
 */
val IncompleteEbtPanError = ElementValidationError(
    detail = "Your EBT card number is incomplete."
)

/**
 * A type of [ElementValidationError] that is thrown when a customer submits an invalid
 * EBT Card number.
 */
val InvalidEbtPanError = ElementValidationError(
    detail = "Your EBT card number is invalid."
)

/**
 * A type of [ElementValidationError] that is thrown when a customer submits an EBT Card number that
 * is too long.
 */
val TooLongEbtPanError = ElementValidationError(
    // in theory the view layer should prevent this
    // validation state from occurring by dynamically restricting
    // the input length based on the IIN.
    // We include this for completeness
    detail = "Your EBT card number is too long."
)

// PIN Input Errors
/**
 * A type of [ElementValidationError] that is thrown when a customer submits an incomplete
 * EBT Card PIN.
 */
val IncompleteEbtPinError = ElementValidationError(
    detail = "Your EBT card PIN is incomplete."
)

/**
 * A type of [ElementValidationError] that is thrown when a customer submits an invalid EBT Card PIN.
 */
val WrongEbtPinError = ElementValidationError(
    detail = "Invalid EBT card PIN entered. Please enter your 4-digit PIN."
)
