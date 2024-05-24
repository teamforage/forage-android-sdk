package com.joinforage.forage.android.pos.ui.element

import com.joinforage.forage.android.core.ui.element.ElementValidationError
import com.joinforage.forage.android.core.ui.element.state.PinElementState
import com.joinforage.forage.android.core.ui.element.state.PinElementStateManager

interface PosPinElementState : PinElementState {
    val length: Int
}

internal data class PosPinElementStateDto(
    override val isFocused: Boolean,
    override val isBlurred: Boolean,
    override val isEmpty: Boolean,
    override val isValid: Boolean,
    override val isComplete: Boolean,
    override val validationError: ElementValidationError?,
    override val length: Int
) : PosPinElementState

internal val INITIAL_POS_PIN_ELEMENT_STATE = PosPinElementStateDto(
    isFocused = false,
    isBlurred = true,
    isEmpty = true,
    isValid = false,
    isComplete = false,
    validationError = null,
    length = 0
)

internal class PosPinElementStateManager(state: PosPinElementState) : PinElementStateManager<PosPinElementState>(state) {
    private var pinTextLength: Int = state.length

    override fun getState(): PosPinElementState {
        return PosPinElementStateDto(
            isFocused = this.isFocused,
            isBlurred = this.isBlurred,
            isEmpty = this.isEmpty,
            isValid = this.isValid,
            isComplete = this.isComplete,
            validationError = this.validationError,
            length = pinTextLength
        )
    }

    fun handleChangeEvent(isComplete: Boolean, isEmpty: Boolean, length: Int) {
        // update the pin length before calling super handleChangeEvent
        // so that the state gets passed with most recent pin length
        this.pinTextLength = length
        super.handleChangeEvent(isComplete, isEmpty)
    }

    companion object {
        fun forEmptyInput(): PosPinElementStateManager {
            return PosPinElementStateManager(INITIAL_POS_PIN_ELEMENT_STATE)
        }
    }
}
