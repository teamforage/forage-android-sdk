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
                    val parsedStyles = parseStyles(context, attrs)

                    _editText = EditText(context, null, parsedStyles.textInputLayoutStyleAttribute).apply {
                        layoutParams =
                            LinearLayout.LayoutParams(
                                parsedStyles.inputWidth,
                                parsedStyles.inputHeight
                            )

                        setTextIsSelectable(true)
                        isSingleLine = true

                        val maxLength = 4
                        filters = arrayOf(InputFilter.LengthFilter(maxLength))

                        if (parsedStyles.textColor != Color.BLACK) {
                            setTextColor(parsedStyles.textColor)
                        }

                        if (parsedStyles.textSize != -1f) {
                            setTextSize(TypedValue.COMPLEX_UNIT_PX, parsedStyles.textSize)
                        }

                        inputType =
                            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD

                        gravity = Gravity.CENTER
                        hint = parsedStyles.hint
                        setHintTextColor(parsedStyles.hintTextColor)

                        val customBackground = GradientDrawable().apply {
                            setPaddingRelative(20, 20, 20, 20)
                            shape = GradientDrawable.RECTANGLE
                            cornerRadii = floatArrayOf(
                                parsedStyles.boxCornerRadiusTopStart,
                                parsedStyles.boxCornerRadiusTopStart,
                                parsedStyles.boxCornerRadiusTopEnd,
                                parsedStyles.boxCornerRadiusTopEnd,
                                parsedStyles.boxCornerRadiusBottomStart,
                                parsedStyles.boxCornerRadiusBottomStart,
                                parsedStyles.boxCornerRadiusBottomEnd,
                                parsedStyles.boxCornerRadiusBottomEnd
                            )
                            setStroke(5, parsedStyles.boxStrokeColor)
                            setColor(parsedStyles.boxBackgroundColor)
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
