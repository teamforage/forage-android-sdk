package com.joinforage.forage.android.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.basistheory.android.view.TextElement
import com.basistheory.android.view.mask.ElementMask
import com.verygoodsecurity.vgscollect.widget.VGSEditText

internal class BTVaultWrapper @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VaultWrapper(context, attrs, defStyleAttr) {
    private var _internalTextElement: TextElement

    init {
        context.obtainStyledAttributes(attrs, com.joinforage.forage.android.R.styleable.ForagePINEditText, defStyleAttr, 0)
            .apply {
                val textInputLayoutStyleAttribute =
                    getResourceId(com.joinforage.forage.android.R.styleable.ForagePINEditText_pinInputLayoutStyle, 0)
                val boxStrokeColor = getColor(
                    com.joinforage.forage.android.R.styleable.ForagePINEditText_boxStrokeColor,
                    getThemeAccentColor(context)
                )
                val defRadius = resources.getDimension(com.joinforage.forage.android.R.dimen.default_horizontal_field)
                val boxCornerRadius =
                    getDimension(com.joinforage.forage.android.R.styleable.ForagePINEditText_boxCornerRadius, defRadius)
                val boxCornerRadiusTopStart = getBoxCornerRadiusTopStart(boxCornerRadius)
                val boxCornerRadiusTopEnd = getBoxCornerRadiusTopEnd(boxCornerRadius)
                val boxCornerRadiusBottomStart = getBoxCornerRadiusBottomStart(boxCornerRadius)
                val boxCornerRadiusBottomEnd = getBoxCornerRadiusBottomEnd(boxCornerRadius)
                val hintTextColorVal =
                    getColor(com.joinforage.forage.android.R.styleable.ForagePINEditText_hintTextColor, getThemeAccentColor(context))

                try {
                    _internalTextElement = TextElement(context, null, textInputLayoutStyleAttribute).apply {
                        layoutParams =
                            LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                        inputType = com.basistheory.android.model.InputType.NUMBER_PASSWORD
                        val digit = Regex("""\d""")
                        mask = ElementMask(listOf(digit, digit, digit, digit))
                        hint = getString(com.joinforage.forage.android.R.styleable.ForagePINEditText_hint)
                        hintTextColor = hintTextColorVal
                        textSize = getDimension(com.joinforage.forage.android.R.styleable.ForagePINEditText_textSize, -1f)
                        textColor = getColor(com.joinforage.forage.android.R.styleable.ForagePINEditText_textColor, Color.BLACK)
                        var customBackground = GradientDrawable().apply {
                            setPaddingRelative(20, 20, 20, 20)
                            shape = GradientDrawable.RECTANGLE
                            cornerRadii = floatArrayOf(boxCornerRadiusTopStart, boxCornerRadiusTopStart, boxCornerRadiusTopEnd, boxCornerRadiusTopEnd, boxCornerRadiusBottomStart, boxCornerRadiusBottomStart, boxCornerRadiusBottomEnd, boxCornerRadiusBottomEnd)
                            setStroke(5, boxStrokeColor)
                        }
                        background = customBackground
                    }
                } finally {
                    recycle()
                }
            }
    }

    override fun getVGSEditText(): VGSEditText {
        throw RuntimeException("Unimplemented for this vault!")
    }

    override fun getTextElement(): TextElement {
        return _internalTextElement
    }

    override fun getUnderlying(): View {
        return _internalTextElement
    }

    override var isValid: Boolean = false
        get() = _internalTextElement.isValid

    override var isEmpty: Boolean = false
        get() = _internalTextElement.isEmpty

    override var typeface: Typeface?
        get() = _internalTextElement.typeface
        set(value) {
            _internalTextElement.typeface = value
        }

    override fun setTextColor(textColor: Int) {
        _internalTextElement.textColor = textColor
    }

    override fun setTextSize(textSize: Float) {
        _internalTextElement.textSize = textSize
    }

    override fun setHint(hint: String) {
        _internalTextElement.hint = hint
    }

    override fun setHintTextColor(hintTextColor: Int) {
        _internalTextElement.hintTextColor = hintTextColor
    }

    override fun focus() {
        _internalTextElement.requestFocus()
    }
}
