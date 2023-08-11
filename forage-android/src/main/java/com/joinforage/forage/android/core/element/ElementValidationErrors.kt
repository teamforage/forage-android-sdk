package com.joinforage.forage.android.core.element

data class ElementValidationError(
    val detail: String
)

// PAN Input Errors
val IncompleteEbtPanError = ElementValidationError(
    detail = "Your EBT card number is incomplete."
)
val InvalidEbtPanError = ElementValidationError(
    detail = "Your EBT card number is invalid."
)
val TooLongEbtPanError = ElementValidationError(
    // in theory the view layer should prevent this
    // validation state from occurring by dynamically restricting
    // the input length based on the IIN.
    // We include this for completeness
    detail = "Your EBT card number is too long."
)

// PIN Input Errors
val IncompleteEbtPinError = ElementValidationError(
    detail = "Your EBT card PIN is incomplete."
)
val WrongEbtPinError = ElementValidationError(
    detail = "Invalid EBT card PIN entered. Please enter your 4-digit PIN."
)
