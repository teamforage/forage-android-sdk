package com.joinforage.forage.android.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputFilter
import android.text.InputType
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import com.basistheory.android.view.TextElement
import com.joinforage.forage.android.R
import com.joinforage.forage.android.core.element.state.PinElementStateManager
import com.verygoodsecurity.vgscollect.widget.VGSEditText

internal class ForageVaultWrapper @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VaultWrapper(context, attrs, defStyleAttr) {
    private val _editText: EditText
    override val manager: PinElementStateManager = PinElementStateManager.forEmptyInput()

    init {
        context.obtainStyledAttributes(attrs, R.styleable.ForagePINEditText, defStyleAttr, 0)
            .apply {
                try {
                    val textInputLayoutStyleAttribute =
                        getResourceId(R.styleable.ForagePINEditText_pinInputLayoutStyle, 0)
                    val boxStrokeColor = getColor(
                        R.styleable.ForagePINEditText_pinBoxStrokeColor,
                        getThemeAccentColor(context)
                    )
                    val boxBackgroundColor = getColor(R.styleable.ForagePINEditText_boxBackgroundColor, Color.TRANSPARENT)

                    val textSize = getDimension(R.styleable.ForagePINEditText_textSize, -1f)
                    val textColor = getColor(R.styleable.ForagePINEditText_textColor, Color.BLACK)

                    val inputWidth: Int = getDimensionPixelSize(R.styleable.ForagePINEditText_inputWidth, ViewGroup.LayoutParams.MATCH_PARENT)
                    val inputHeight: Int = getDimensionPixelSize(R.styleable.ForagePINEditText_inputHeight, ViewGroup.LayoutParams.WRAP_CONTENT)

                    val defRadius = resources.getDimension(com.joinforage.forage.android.R.dimen.default_horizontal_field)
                    val boxCornerRadius =
                        getDimension(R.styleable.ForagePINEditText_boxCornerRadius, defRadius)
                    val boxCornerRadiusTopStart = getBoxCornerRadiusTopStart(boxCornerRadius)
                    val boxCornerRadiusTopEnd = getBoxCornerRadiusTopEnd(boxCornerRadius)
                    val boxCornerRadiusBottomStart = getBoxCornerRadiusBottomStart(boxCornerRadius)
                    val boxCornerRadiusBottomEnd = getBoxCornerRadiusBottomEnd(boxCornerRadius)

                    _editText = EditText(context, null, textInputLayoutStyleAttribute).apply {
                        layoutParams =
                            LinearLayout.LayoutParams(
                                inputWidth,
                                inputHeight
                            )

                        setTextIsSelectable(true)
                        isSingleLine = true

                        val maxLength = 4
                        filters = arrayOf(InputFilter.LengthFilter(maxLength))

                        if (textColor != Color.BLACK) {
                            setTextColor(textColor)
                        }

                        if (textSize != -1f) {
                            setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
                        }

                        inputType =
                            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD

                        gravity = Gravity.CENTER
                        hint = getString(R.styleable.ForagePINEditText_hint)
                        setHintTextColor(getColorStateList(R.styleable.ForagePINEditText_hintTextColor))

                        val customBackground = GradientDrawable().apply {
                            setPaddingRelative(20, 20, 20, 20)
                            shape = GradientDrawable.RECTANGLE
                            cornerRadii = floatArrayOf(
                                boxCornerRadiusTopStart,
                                boxCornerRadiusTopStart,
                                boxCornerRadiusTopEnd,
                                boxCornerRadiusTopEnd,
                                boxCornerRadiusBottomStart,
                                boxCornerRadiusBottomStart,
                                boxCornerRadiusBottomEnd,
                                boxCornerRadiusBottomEnd
                            )
                            setStroke(5, boxStrokeColor)
                            setColor(boxBackgroundColor)
                        }
                        background = customBackground
                    }

                    _editText.setOnFocusChangeListener { _, hasFocus ->
                        manager.changeFocus(hasFocus)
                    }
                    val pinTextWatcher = PinTextWatcher(_editText)
                    pinTextWatcher.onInputChangeEvent { isComplete, isEmpty ->
                        manager.handleChangeEvent(isComplete, isEmpty)
                    }
                    _editText.addTextChangedListener(pinTextWatcher)
                } finally {
                    recycle()
                }
            }
    }

    override fun clearText() {
        _editText.setText("")
    }

    override fun getForageTextElement(): EditText {
        return _editText
    }

    override fun getTextElement(): TextElement {
        throw RuntimeException("Unimplemented for this vault!")
    }

    override fun getVGSEditText(): VGSEditText {
        throw RuntimeException("Unimplemented for this vault!")
    }

    override fun getUnderlying(): EditText {
        return _editText
    }

    override var typeface: Typeface?
        get() = _editText.typeface
        set(value) {
            if (value != null) {
                _editText.typeface = value
            }
        }

    override fun setTextColor(textColor: Int) {
        _editText.setTextColor(textColor)
    }

    override fun setTextSize(textSize: Float) {
        _editText.textSize = textSize
    }

    override fun setHint(hint: String) {
        _editText.hint = hint
    }

    override fun setHintTextColor(hintTextColor: Int) {
        _editText.setHintTextColor(hintTextColor)
    }
}
