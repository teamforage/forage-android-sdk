package com.joinforage.forage.android.core.element

/**
 * A model that represents an error related to an incomplete or incorrect customer input.
 * @property detail A customer-facing message that describes the error.
 */
data class ElementValidationError(
    val detail: String
)

// PAN Input Errors
/**
 * A type of [ElementValidationError] that occurs when a customer submits an incomplete
 * EBT Card number.
 */
val IncompleteEbtPanError = ElementValidationError(
    detail = "Your EBT card number is incomplete."
)

/**
 * A type of [ElementValidationError] that occurs when a customer submits an invalid numeric input
 * that fails to meet the expected length constraint.
 */
val InvalidEbtPanError = ElementValidationError(
    detail = "Your EBT card number is invalid."
)

/**
 * **Deprecated** - This error will be removed in a future release.
 * A type of [ElementValidationError] that occurs when a customer submits an EBT Card number that
 * is too long. In theory, this error should never occur.
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
 * A type of [ElementValidationError] that occurs when a customer submits an incomplete
 * EBT Card PIN.
 */
val IncompleteEbtPinError = ElementValidationError(
    detail = "Your EBT card PIN is incomplete."
)

/**
 * A type of [ElementValidationError] that occurs when a customer submits an invalid EBT Card PIN.
 */
val WrongEbtPinError = ElementValidationError(
    detail = "Invalid EBT card PIN entered. Please enter your 4-digit PIN."
)
