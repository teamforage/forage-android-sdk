package com.joinforage.forage.android.core.ui.element.state

import com.joinforage.forage.android.core.ui.element.ElementValidationError

/**
 * An interface that represents the state of the [ForageElement][com.joinforage.forage.android.core.ui.ForageElement]
 * as the customer interacts with it.
 * @property isEmpty Whether the input value of the Element is empty.
 * @property isValid Whether the input text fails any validation checks, with the exception of the
 * target length constraint.
 * @property isComplete Whether the text field of the Element is ready to submit. This is `true`
 * when all input value validation constraints are satisfied.
 * @property validationError An [ElementValidationError], if `isValid` is `false`.
 * @see [EditTextState]
 */
interface ElementState {
    val isEmpty: Boolean
    val isValid: Boolean
    val isComplete: Boolean
    val validationError: ElementValidationError?
}
