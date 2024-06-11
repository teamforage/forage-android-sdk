package com.joinforage.forage.android.core.ui.element.state

import com.joinforage.forage.android.core.ui.element.ElementValidationError

// TODO: docstrings
interface ElementState {
    val isEmpty: Boolean
    val isValid: Boolean
    val isComplete: Boolean
    val validationError: ElementValidationError?
}
