package com.joinforage.forage.android.ui

import android.content.Context
import android.content.res.TypedArray
import android.util.TypedValue
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import com.joinforage.forage.android.model.queryForStateIIN

fun expandDigitsToFormat(pattern: String): (String) -> String {
    val targetCharGroupSizes = pattern.split(" ").map { it.length }
    return { input: String ->
        val acc = listOf<String>() to 0
        val inputEnd = input.length
        val (finalGroups, _) = targetCharGroupSizes.fold(acc) { (groupsAcc, i), groupSize ->
            // don't start group beyond input size
            val start = minOf(i, inputEnd)

            // don't end group beyond input size
            val end = minOf(i + groupSize, inputEnd)

            // create char group of [start, end]
            val charGroup = input.substring(start, end)

            // update accumulator for next iteration
            val newGroupsAcc = groupsAcc + charGroup
            val newIndex = i + groupSize
            newGroupsAcc to newIndex
        }

        // Groups are separated visually with a single " " character
        finalGroups.joinToString(" ").trim()
    }
}

const val MAX_PAN_LENGTH = 19
val space16DigitPAN = expandDigitsToFormat("#### #### #### ####")
val space18DigitPAN = expandDigitsToFormat("###### #### ##### ## #")
val space19DigitPAN = expandDigitsToFormat("###### #### #### ### ##")
val defaultTransform = expandDigitsToFormat("#### #### #### #### ###")

fun stripNonDigits(input: String): String {
    return input.replace(Regex("\\D"), "")
}

internal fun truncateDigitsByStateIIN(digits: String): String {
    val stateIIN = queryForStateIIN(digits)
    val targetLength = stateIIN?.panLength ?: MAX_PAN_LENGTH
    return digits.take(targetLength)
}

fun formatDigitsByStateIIN(digits: String): String {
    val stateIIN = queryForStateIIN(digits)
    val transform = when (stateIIN?.panLength) {
        16 -> space16DigitPAN
        18 -> space18DigitPAN
        19 -> space19DigitPAN
        else -> defaultTransform
    }
    return transform(digits)
}

fun countSpacesBeforePos(pos: Int, str: String): Int {
    // protect against boundary cases
    val end = minOf(pos, str.length)

    // do the logic we actually care about
    return str.substring(0, end).count { it == ' ' }
}
fun getTransformedPosition(pos: Int, oldPrettyText: String, newPrettyText: String): Int {
    // formatted digits include spaces which change  on inserts,
    // copy/paste, etc. We need to ensure we factor any space
    // additions or removals into the cursor's position so the
    // cursor behaves intuitively to users
    val prevSpacesBeforePos = countSpacesBeforePos(pos, oldPrettyText)
    val newSpacesBeforePos = countSpacesBeforePos(pos, newPrettyText)
    val spacesShift = newSpacesBeforePos - prevSpacesBeforePos

    // ensure we are within the newPrettyText's bounds
    val shiftedPos = pos + spacesShift
    return minOf(shiftedPos, newPrettyText.length)
}

fun getThemeAccentColor(context: Context): Int {
    val outValue = TypedValue()
    context.theme.resolveAttribute(android.R.attr.colorAccent, outValue, true)
    return outValue.data
}

fun TypedArray.getBoxCornerRadiusBottomStart(boxCornerRadius: Float): Float {
    val boxCornerRadiusBottomStart =
        getDimension(com.joinforage.forage.android.R.styleable.ForagePANEditText_boxCornerBottomStart, 0f)
    return if (boxCornerRadiusBottomStart == 0f) boxCornerRadius else boxCornerRadiusBottomStart
}

fun TypedArray.getBoxCornerRadiusTopEnd(boxCornerRadius: Float): Float {
    val boxCornerRadiusTopEnd =
        getDimension(com.joinforage.forage.android.R.styleable.ForagePANEditText_boxCornerTopEnd, 0f)
    return if (boxCornerRadiusTopEnd == 0f) boxCornerRadius else boxCornerRadiusTopEnd
}

fun TypedArray.getBoxCornerRadiusBottomEnd(boxCornerRadius: Float): Float {
    val boxCornerRadiusBottomEnd =
        getDimension(com.joinforage.forage.android.R.styleable.ForagePANEditText_boxCornerBottomEnd, 0f)
    return if (boxCornerRadiusBottomEnd == 0f) boxCornerRadius else boxCornerRadiusBottomEnd
}

fun TypedArray.getBoxCornerRadiusTopStart(boxCornerRadius: Float): Float {
    val boxCornerRadiusTopStart =
        getDimension(com.joinforage.forage.android.R.styleable.ForagePANEditText_boxCornerTopStart, 0f)
    return if (boxCornerRadiusTopStart == 0f) boxCornerRadius else boxCornerRadiusTopStart
}

class FormatPanTextWatcher(
    private val editText: EditText
) : TextWatcher {
    private var isProgrammaticChange: Boolean = false
    private var onFormattedChangeEvent: ((String) -> Unit)? = null

    private fun runWithLock(block: () -> Unit) {
        if (isProgrammaticChange) return
        isProgrammaticChange = true
        block()
        isProgrammaticChange = false
    }

    fun onFormattedChangeEvent(callback: (String) -> Unit) {
        onFormattedChangeEvent = callback
    }

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        // a no-op for now
    }

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        // a no-op for now
    }

    override fun afterTextChanged(editable: Editable) {
        // within this method we programmatically change
        // the EditText's text. To avoid an infinite loop
        // we use a lock
        runWithLock {
            // save the cursor's end position for later
            val end = editText.selectionEnd

            // save for frequent reuse
            val rawInput = editable.toString()

            // the TextEdit's content have just been changed and need
            // to be reformatted. Let's do that now
            val onlyDigits = stripNonDigits(rawInput)
            val truncatedDigits = truncateDigitsByStateIIN(onlyDigits)
            val newText = formatDigitsByStateIIN(truncatedDigits)

            // replace the current (unformatted) content of the
            // TextEdit with properly formatted content
            editable.replace(0, rawInput.length, newText)

            // adjust the cursor's position to make sure it moves
            // intuitively for users
            val newEnd = getTransformedPosition(end, rawInput, newText)
            editText.setSelection(newEnd)

            // fire the change event for the merchant now that we've
            // established the new (formatted) value
            onFormattedChangeEvent?.invoke(newText)
        }
    }
}
