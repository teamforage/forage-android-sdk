package com.joinforage.forage.android.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.basistheory.android.view.TextElement
import com.basistheory.android.view.mask.ElementMask
import com.joinforage.forage.android.core.element.state.PinElementStateManager
import com.verygoodsecurity.vgscollect.widget.VGSEditText

internal class BTVaultWrapper @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VaultWrapper(context, attrs, defStyleAttr) {
    private var _internalTextElement: TextElement
    override val manager: PinElementStateManager = PinElementStateManager.forEmptyInput()

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

                val inputWidth: Int = this.getDimensionPixelSize(com.joinforage.forage.android.R.styleable.ForagePINEditText_input_width, ViewGroup.LayoutParams.MATCH_PARENT)
                val inputHeight: Int = this.getDimensionPixelSize(com.joinforage.forage.android.R.styleable.ForagePINEditText_input_height, ViewGroup.LayoutParams.WRAP_CONTENT)

                try {
                    _internalTextElement = TextElement(context, null, textInputLayoutStyleAttribute).apply {
                        layoutParams =
                            LinearLayout.LayoutParams(
                                inputWidth,
                                inputHeight
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

                    // Basis Theory keeps a list of as many listeners as you want
                    // for a given event see here: https://tinyurl.com/yc6wzt8v
                    // We only support attaching a single listener
                    // to any event for simplicity. In order work with BT's
                    // append-only subscribe protocol, we will only ever subscribe
                    // a single listener to Basis Theory during initialization
                    // and we will use a mutating reference that only points
                    // the most recent event listener
                    _internalTextElement.addFocusEventListener { manager.focus() }
                    _internalTextElement.addBlurEventListener { manager.blur() }
                    _internalTextElement.addChangeEventListener { state ->
                        // map Basis Theory's event representation to Forage's
                        manager.handleChangeEvent(
                            isComplete = state.isComplete,
                            isEmpty = state.isEmpty
                        )
                    }
                } finally {
                    recycle()
                }
            }
    }

    override fun clearText() {
        _internalTextElement.setText("")
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
}
