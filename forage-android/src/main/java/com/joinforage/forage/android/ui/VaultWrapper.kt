package com.joinforage.forage.android.ui

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.FrameLayout
import com.basistheory.android.view.TextElement
import com.joinforage.forage.android.core.element.SimpleElementListener
import com.joinforage.forage.android.core.element.StatefulElementListener
import com.joinforage.forage.android.core.element.state.PinElementState
import com.joinforage.forage.android.core.element.state.PinElementStateManager
import com.verygoodsecurity.vgscollect.widget.VGSEditText

internal abstract class VaultWrapper @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    abstract var typeface: Typeface?

    // mutable references to event listeners. We use mutable
    // references because the implementations of our vaults
    // require that we are only able to ever pass a single
    // monolithic event within init call. This is mutability
    // allows us simulate setting and overwriting a listener
    // with every set call
    internal abstract val manager: PinElementStateManager

    abstract fun clearText()

    abstract fun setTextColor(textColor: Int)
    abstract fun setTextSize(textSize: Float)
    abstract fun setHint(hint: String)
    abstract fun setHintTextColor(hintTextColor: Int)
    abstract fun getUnderlying(): View
    abstract fun getVGSEditText(): VGSEditText
    abstract fun getTextElement(): TextElement

    fun getThemeAccentColor(context: Context): Int {
        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorAccent, outValue, true)
        return outValue.data
    }

    fun TypedArray.getBoxCornerRadiusBottomStart(boxCornerRadius: Float): Float {
        val boxCornerRadiusBottomStart =
            getDimension(com.joinforage.forage.android.R.styleable.ForagePINEditText_boxCornerRadiusBottomStart, 0f)
        return if (boxCornerRadiusBottomStart == 0f) boxCornerRadius else boxCornerRadiusBottomStart
    }

    fun TypedArray.getBoxCornerRadiusTopEnd(boxCornerRadius: Float): Float {
        val boxCornerRadiusTopEnd =
            getDimension(com.joinforage.forage.android.R.styleable.ForagePINEditText_boxCornerRadiusTopEnd, 0f)
        return if (boxCornerRadiusTopEnd == 0f) boxCornerRadius else boxCornerRadiusTopEnd
    }

    fun TypedArray.getBoxCornerRadiusBottomEnd(boxCornerRadius: Float): Float {
        val boxCornerRadiusBottomEnd =
            getDimension(com.joinforage.forage.android.R.styleable.ForagePINEditText_boxCornerRadiusBottomEnd, 0f)
        return if (boxCornerRadiusBottomEnd == 0f) boxCornerRadius else boxCornerRadiusBottomEnd
    }

    fun TypedArray.getBoxCornerRadiusTopStart(boxCornerRadius: Float): Float {
        val boxCornerRadiusTopStart =
            getDimension(com.joinforage.forage.android.R.styleable.ForagePINEditText_boxCornerRadiusTopStart, 0f)
        return if (boxCornerRadiusTopStart == 0f) boxCornerRadius else boxCornerRadiusTopStart
    }

    fun setOnFocusEventListener(l: SimpleElementListener) {
        manager.setOnFocusEventListener(l)
    }

    fun setOnBlurEventListener(l: SimpleElementListener) {
        manager.setOnBlurEventListener(l)
    }

    fun setOnChangeEventListener(l: StatefulElementListener<PinElementState>) {
        manager.setOnChangeEventListener(l)
    }
}
