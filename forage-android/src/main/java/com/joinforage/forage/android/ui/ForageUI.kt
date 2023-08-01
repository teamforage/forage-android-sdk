package com.joinforage.forage.android.ui

import android.graphics.Typeface

/**
 * ObservableState interface represents the state of an input
 * that Forage wants to expose during events or statically
 * as input instance attributes. NOTE: that this state is
 * read-only `val`s
 */
interface ObservableState {
    /**
     * isFocused is true if the input is focused, false otherwise.
     */
    val isFocused: Boolean

    /**
     * isBlurred is true if the input is blurred, false otherwise.
     */
    val isBlurred: Boolean

    /**
     * isEmpty is true if the input has no text, false otherwise.
     */
    val isEmpty: Boolean

    /**
     * isValid is true when the input text does not fail any validation
     * checks with the exception of target length; false if any
     * validation checks other than target length fail.
     */
    val isValid: Boolean

    /**
     * isComplete is true when all validation checks pass and the input
     * is ready to be submitted.
     */
    val isComplete: Boolean
}

/**
 * Appearance interface represents higher visual characteristics that
 * apply to every Forage input and are not specific to a single input.
 * NOTE: that this state is read-write `var`s
 */
interface Appearance {
    /**
     * textColor specifies the color of the inputs' text.
     */
    var textColor: Int

    /**
     * hintTextColor specifies the color of the hint/placeholder text of the inputs.
     */
    var hintTextColor: Int

    /**
     * typeface specifies the font of the inputs.
     */
    var typeface: Typeface?
}

/**
 * Style interface represents visual characteristics that require
 * input-specific customization and would not make sense to apply
 * to all inputs. NOTE: that this state is read-write `var`s
 */
interface Style {
    /**
     * textSize specifies the size of the input's text.
     */
    var textSize: Float

    /**
     * hint represents the hint/placeholder text of the input.
     */
    var hint: String
}

/**
 * EventListener is a function that takes an ObservableState and returns Unit.
 *
 * @param ObservableState The current observable state of a ForageInputElement
 * at the time the event fires.
 *
 * Usage:
 * myElement.addChangeEventListener { state ->
 *     // handle event with the given state
 * }
 */
typealias EventListener = (ObservableState) -> Unit

interface ForageUI {
    var isValid: Boolean
    var isEmpty: Boolean
    var typeface: Typeface?
    fun setTextColor(textColor: Int)
    fun setTextSize(textSize: Float)
    fun setHint(hint: String)
    fun setHintTextColor(hintTextColor: Int)
}
