package com.joinforage.forage.android.ui

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.basistheory.android.view.TextElement
import com.basistheory.android.view.mask.ElementMask
import com.joinforage.forage.android.network.ForageConstants
import com.verygoodsecurity.vgscollect.widget.VGSEditText

interface VaultWrapper {
    var isValid: Boolean
    var isEmpty: Boolean
    fun setTextColor(textColor: Int)
    fun setTextSize(textSize: Float)
    var typeface: Typeface?
    fun setHint(hint: String)
    fun setHintTextColor(hintTextColor: Int)
    fun getUnderlying(): View
    fun getVGSEditText(): VGSEditText
    fun getTextElement(): TextElement
}

internal class VGSVaultWrapper @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VaultWrapper, FrameLayout(context, attrs, defStyleAttr) {
    private var _internalEditText: VGSEditText

    init {
        context.obtainStyledAttributes(attrs, com.joinforage.forage.android.R.styleable.ForagePINEditText, defStyleAttr, 0)
            .apply {
                try {
                    val hint = getString(com.joinforage.forage.android.R.styleable.ForagePINEditText_hint)
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
                    val hintTextColor =
                        getColorStateList(com.joinforage.forage.android.R.styleable.ForagePINEditText_hintTextColor)
                    val textSize = getDimension(com.joinforage.forage.android.R.styleable.ForagePINEditText_textSize, -1f)
                    val textColor = getColor(com.joinforage.forage.android.R.styleable.ForagePINEditText_textColor, Color.BLACK)

                    _internalEditText = VGSEditText(context, null, textInputLayoutStyleAttribute).apply {
                        layoutParams =
                            LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )

                        setHint(hint)
                        hintTextColor?.let {
                            setHintTextColor(hintTextColor)
                        }

                        var customBackground = GradientDrawable().apply {
                            // TODO: Don't think this needs to be included for VGS
                            setPaddingRelative(20, 20, 20, 20)
                            shape = GradientDrawable.RECTANGLE
                            cornerRadii = floatArrayOf(boxCornerRadiusTopStart, boxCornerRadiusTopStart, boxCornerRadiusTopEnd, boxCornerRadiusTopEnd, boxCornerRadiusBottomStart, boxCornerRadiusBottomStart, boxCornerRadiusBottomEnd, boxCornerRadiusBottomEnd)
                            setStroke(5, boxStrokeColor)
                        }
                        background = customBackground

                        setTextColor(textColor)
                        setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)

                        // Constant VGS configuration. Can't be externally altered!
                        setFieldName(ForageConstants.VGS.PIN_FIELD_NAME)
                        setMaxLength(4)
                        setInputType(android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD)
                        // TODO: Consider exposing Padding as an editable field
                        setPadding(20, 20, 20, 20)
                    }
                } finally {
                    recycle()
                }
            }
    }

    override fun getVGSEditText(): VGSEditText {
        return _internalEditText
    }

    override fun getTextElement(): TextElement {
        throw RuntimeException("Unimplemented for this vault!")
    }

    override fun getUnderlying(): VGSEditText {
        return _internalEditText
    }

    override var isValid: Boolean = false
        get() = _internalEditText.getState()?.isValid == true

    override var isEmpty: Boolean = false
        get() = _internalEditText.getState()?.isEmpty == true

    override fun setTextColor(textColor: Int) {
        _internalEditText.setTextColor(textColor)
    }

    override fun setTextSize(textSize: Float) {
        _internalEditText.setTextSize(textSize)
    }

    override var typeface: Typeface?
        get() = _internalEditText.getTypeface()
        set(value) {
            if (value != null) {
                _internalEditText.setTypeface(value)
            }
        }

    override fun setHint(hint: String) {
        _internalEditText.setHint(hint)
    }

    override fun setHintTextColor(hintTextColor: Int) {
        _internalEditText.setHintTextColor(hintTextColor)
    }
}

internal class BTVaultWrapper @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VaultWrapper, FrameLayout(context, attrs, defStyleAttr) {
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

    override fun setTextColor(textColor: Int) {
        _internalTextElement.textColor = textColor
    }

    override fun setTextSize(textSize: Float) {
        _internalTextElement.textSize = textSize
    }

    override var typeface: Typeface?
        get() = _internalTextElement.typeface
        set(value) {
            _internalTextElement.typeface = value
        }

    override fun setHint(hint: String) {
        _internalTextElement.hint = hint
    }

    override fun setHintTextColor(hintTextColor: Int) {
        _internalTextElement.hintTextColor = hintTextColor
    }
}

private fun getThemeAccentColor(context: Context): Int {
    val outValue = TypedValue()
    context.theme.resolveAttribute(android.R.attr.colorAccent, outValue, true)
    return outValue.data
}

private fun TypedArray.getBoxCornerRadiusBottomStart(boxCornerRadius: Float): Float {
    val boxCornerRadiusBottomStart =
        getDimension(com.joinforage.forage.android.R.styleable.ForagePINEditText_boxCornerRadiusBottomStart, 0f)
    return if (boxCornerRadiusBottomStart == 0f) boxCornerRadius else boxCornerRadiusBottomStart
}

private fun TypedArray.getBoxCornerRadiusTopEnd(boxCornerRadius: Float): Float {
    val boxCornerRadiusTopEnd =
        getDimension(com.joinforage.forage.android.R.styleable.ForagePINEditText_boxCornerRadiusTopEnd, 0f)
    return if (boxCornerRadiusTopEnd == 0f) boxCornerRadius else boxCornerRadiusTopEnd
}

private fun TypedArray.getBoxCornerRadiusBottomEnd(boxCornerRadius: Float): Float {
    val boxCornerRadiusBottomEnd =
        getDimension(com.joinforage.forage.android.R.styleable.ForagePINEditText_boxCornerRadiusBottomEnd, 0f)
    return if (boxCornerRadiusBottomEnd == 0f) boxCornerRadius else boxCornerRadiusBottomEnd
}

private fun TypedArray.getBoxCornerRadiusTopStart(boxCornerRadius: Float): Float {
    val boxCornerRadiusTopStart =
        getDimension(com.joinforage.forage.android.R.styleable.ForagePINEditText_boxCornerRadiusTopStart, 0f)
    return if (boxCornerRadiusTopStart == 0f) boxCornerRadius else boxCornerRadiusTopStart
}
