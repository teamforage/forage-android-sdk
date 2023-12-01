package com.joinforage.forage.android.core.element.state

import com.joinforage.forage.android.core.element.ElementValidationError
import com.joinforage.forage.android.core.element.SimpleElementListener
import com.joinforage.forage.android.core.element.StatefulElementListener

internal abstract class ElementStateManager<InputDetails>(
    private var isFocused: Boolean,
    private var isBlurred: Boolean,
    internal var isEmpty: Boolean,
    internal var isValid: Boolean,
    internal var isComplete: Boolean,
    internal var validationError: ElementValidationError?,
    internal var details: InputDetails
) {
    private var onFocusEventListener: SimpleElementListener? = null
    private var onBlurEventListener: SimpleElementListener? = null
    internal var onChangeEventListener: StatefulElementListener<InputDetails>? = null

    internal constructor(state: ElementState<InputDetails>) : this(
        isFocused = state.isFocused,
        isBlurred = state.isBlurred,
        isEmpty = state.isEmpty,
        isValid = state.isValid,
        isComplete = state.isComplete,
        validationError = state.validationError,
        details = state.details
    )

    fun getState(): ElementState<InputDetails> {
        return ElementState(
            isFocused = isFocused,
            isBlurred = isBlurred,
            isEmpty = isEmpty,
            isValid = isValid,
            isComplete = isComplete,
            validationError = validationError,
            details = details
        )
    }

    fun setOnFocusEventListener(l: SimpleElementListener) {
        onFocusEventListener = l
    }

    fun setOnBlurEventListener(l: SimpleElementListener) {
        onBlurEventListener = l
    }

    fun setOnChangeEventListener(l: StatefulElementListener<InputDetails>) {
        onChangeEventListener = l
    }

    fun changeFocus(hasFocus: Boolean) {
        isFocused = hasFocus
        isBlurred = !hasFocus
        if (hasFocus) {
            onFocusEventListener?.invoke()
        } else {
            onBlurEventListener?.invoke()
        }
    }

    fun focus() {
        changeFocus(true)
    }

    fun blur() {
        changeFocus(false)
    }
}
