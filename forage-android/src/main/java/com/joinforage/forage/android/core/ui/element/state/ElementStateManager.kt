package com.joinforage.forage.android.core.ui.element.state

import com.joinforage.forage.android.core.ui.element.ElementValidationError
import com.joinforage.forage.android.core.ui.element.SimpleElementListener
import com.joinforage.forage.android.core.ui.element.StatefulElementListener

internal abstract class ElementStateManager<T : ElementState>(
    // private because only this class should house focus / blur logic
    private var _isFocused: Boolean,
    private var _isBlurred: Boolean,

    // internal because subclasses will define the logic for these
    internal var isEmpty: Boolean,
    internal var isValid: Boolean,
    internal var isComplete: Boolean,
    internal var validationError: ElementValidationError?
) {
    private var onFocusEventListener: SimpleElementListener? = null
    private var onBlurEventListener: SimpleElementListener? = null
    internal var onChangeEventListener: StatefulElementListener<T>? = null

    internal val isFocused
        get() = _isFocused

    internal val isBlurred
        get() = _isBlurred

    internal constructor(state: T) : this(
        _isFocused = state.isFocused,
        _isBlurred = state.isBlurred,
        isEmpty = state.isEmpty,
        isValid = state.isValid,
        isComplete = state.isComplete,
        validationError = state.validationError
    )

    internal abstract fun getState(): T

    fun setOnFocusEventListener(l: SimpleElementListener) {
        onFocusEventListener = l
    }

    fun setOnBlurEventListener(l: SimpleElementListener) {
        onBlurEventListener = l
    }

    fun setOnChangeEventListener(l: StatefulElementListener<T>) {
        onChangeEventListener = l
    }

    fun changeFocus(hasFocus: Boolean) {
        _isFocused = hasFocus
        _isBlurred = !hasFocus
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
