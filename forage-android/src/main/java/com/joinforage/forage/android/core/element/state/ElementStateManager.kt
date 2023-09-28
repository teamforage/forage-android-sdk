package com.joinforage.forage.android.core.element.state

import com.joinforage.forage.android.core.element.ElementValidationError
import com.joinforage.forage.android.core.element.SimpleElementListener
import com.joinforage.forage.android.core.element.StatefulElementListener

internal abstract class ElementStateManager(
    private var isFocused: Boolean,
    private var isBlurred: Boolean,
    internal var isEmpty: Boolean,
    internal var isValid: Boolean,
    internal var isComplete: Boolean,
    internal var validationError: ElementValidationError?
) {
    private var onFocusEventListener: SimpleElementListener? = null
    private var onBlurEventListener: SimpleElementListener? = null
    internal var onChangeEventListener: StatefulElementListener? = null

    internal constructor(state: ElementState) : this(
        isFocused = state.isFocused,
        isBlurred = state.isBlurred,
        isEmpty = state.isEmpty,
        isValid = state.isValid,
        isComplete = state.isComplete,
        validationError = state.validationError
    )

    fun getState(): ElementState {
        return ElementState(
            isFocused = isFocused,
            isBlurred = isBlurred,
            isEmpty = isEmpty,
            isValid = isValid,
            isComplete = isComplete,
            validationError = validationError
        )
    }

    fun setOnFocusEventListener(l: SimpleElementListener) {
        onFocusEventListener = l
    }

    fun setOnBlurEventListener(l: SimpleElementListener) {
        onBlurEventListener = l
    }

    fun setOnChangeEventListener(l: StatefulElementListener) {
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
