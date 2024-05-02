package com.joinforage.forage.android.ecom.ui.vault

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import com.basistheory.android.view.TextElement
import com.joinforage.forage.android.R
import com.joinforage.forage.android.core.services.launchdarkly.VaultType
import com.joinforage.forage.android.core.ui.element.SimpleElementListener
import com.joinforage.forage.android.core.ui.element.StatefulElementListener
import com.joinforage.forage.android.core.ui.element.state.PinElementState
import com.joinforage.forage.android.core.ui.element.state.PinElementStateManager
import com.joinforage.forage.android.core.ui.getBoxCornerRadius
import com.verygoodsecurity.vgscollect.widget.VGSEditText

internal data class ParsedStyles(
    val textInputLayoutStyleAttribute: Int,
    val boxStrokeColor: Int,
    val boxBackgroundColor: Int,
    val boxCornerRadiusTopStart: Float,
    val boxCornerRadiusTopEnd: Float,
    val boxCornerRadiusBottomStart: Float,
    val boxCornerRadiusBottomEnd: Float,
    val hint: String?,
    val hintTextColor: ColorStateList?,
    val inputWidth: Int,
    val inputHeight: Int,
    val textSize: Float,
    val textColor: Int
)

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
    abstract fun getForageTextElement(): EditText
    abstract fun getVaultType(): VaultType

    fun parseStyles(context: Context, attrs: AttributeSet?): ParsedStyles {
        val defaultRadius = resources.getDimension(R.dimen.default_horizontal_field)
        val typedArray: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.ForagePINEditText)
        val boxCornerRadius = typedArray.getDimension(R.styleable.ForagePINEditText_boxCornerRadius, defaultRadius)

        try {
            return ParsedStyles(
                textInputLayoutStyleAttribute = typedArray.getResourceId(R.styleable.ForagePINEditText_pinInputLayoutStyle, 0),
                boxStrokeColor = typedArray.getColor(R.styleable.ForagePINEditText_pinBoxStrokeColor, getThemeAccentColor(context)),
                boxBackgroundColor = typedArray.getColor(R.styleable.ForagePINEditText_boxBackgroundColor, Color.TRANSPARENT),
                boxCornerRadiusTopStart = typedArray.getBoxCornerRadius(R.styleable.ForagePINEditText_boxCornerRadiusTopStart, boxCornerRadius),
                boxCornerRadiusTopEnd = typedArray.getBoxCornerRadius(R.styleable.ForagePINEditText_boxCornerRadiusTopEnd, boxCornerRadius),
                boxCornerRadiusBottomStart = typedArray.getBoxCornerRadius(R.styleable.ForagePINEditText_boxCornerRadiusBottomStart, boxCornerRadius),
                boxCornerRadiusBottomEnd = typedArray.getBoxCornerRadius(R.styleable.ForagePINEditText_boxCornerRadiusBottomEnd, boxCornerRadius),
                hint = typedArray.getString(R.styleable.ForagePINEditText_hint),
                hintTextColor = typedArray.getColorStateList(R.styleable.ForagePINEditText_hintTextColor),
                inputWidth = typedArray.getDimensionPixelSize(R.styleable.ForagePINEditText_inputWidth, ViewGroup.LayoutParams.MATCH_PARENT),
                inputHeight = typedArray.getDimensionPixelSize(R.styleable.ForagePINEditText_inputHeight, ViewGroup.LayoutParams.WRAP_CONTENT),
                textSize = typedArray.getDimension(R.styleable.ForagePINEditText_textSize, -1f),
                textColor = typedArray.getColor(R.styleable.ForagePINEditText_textColor, Color.BLACK)
            )
        } finally {
            typedArray.recycle()
        }
    }

    fun getThemeAccentColor(context: Context): Int {
        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorAccent, outValue, true)
        return outValue.data
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
