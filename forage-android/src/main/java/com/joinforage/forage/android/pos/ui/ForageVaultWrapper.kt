package com.joinforage.forage.android.pos.ui

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputFilter
import android.text.InputType
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.content.getSystemService
import com.joinforage.forage.android.R
import com.joinforage.forage.android.core.services.EnvConfig
import com.joinforage.forage.android.core.services.VaultType
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.services.vault.AbstractVaultSubmitter
import com.joinforage.forage.android.core.services.vault.SecurePinCollector
import com.joinforage.forage.android.core.ui.VaultWrapper
import com.joinforage.forage.android.core.ui.element.state.PinElementStateManager
import com.joinforage.forage.android.core.ui.getBoxCornerRadius
import com.joinforage.forage.android.core.ui.textwatcher.PinTextWatcher
import com.joinforage.forage.android.pos.services.vault.rosetta.ForagePinSubmitter

internal class ForageVaultWrapper @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VaultWrapper(context, attrs, defStyleAttr) {
    override val vaultType: VaultType = VaultType.FORAGE_VAULT_TYPE
    private val _editText: EditText
    override fun showKeyboard() {
        val imm = context.getSystemService<InputMethodManager>()
        imm!!.showSoftInput(_editText, 0)
    }

    override val manager: PinElementStateManager = PinElementStateManager.forEmptyInput()

    override fun getVaultSubmitter(
        envConfig: EnvConfig,
        logger: Log
    ): AbstractVaultSubmitter = ForagePinSubmitter(
        _editText,
        object : SecurePinCollector {
            override fun clearText() {
                clearText()
            }
            override fun isComplete(): Boolean = manager.isComplete
        },
        envConfig,
        logger
    )

    init {
        context.obtainStyledAttributes(attrs, R.styleable.ForagePINEditText, defStyleAttr, 0)
            .apply {
                try {
                    val defaultRadius = resources.getDimension(R.dimen.default_horizontal_field)
                    val typedArray: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.ForagePINEditText)
                    val boxCornerRadius = typedArray.getDimension(R.styleable.ForagePINEditText_boxCornerRadius, defaultRadius)

                    try {
                        val textInputLayoutStyleAttribute = typedArray.getResourceId(R.styleable.ForagePINEditText_pinInputLayoutStyle, 0)
                        val boxStrokeColor = typedArray.getColor(R.styleable.ForagePINEditText_pinBoxStrokeColor, getThemeAccentColor(context))
                        val boxBackgroundColor = typedArray.getColor(R.styleable.ForagePINEditText_boxBackgroundColor, Color.TRANSPARENT)
                        val boxCornerRadiusTopStart = typedArray.getBoxCornerRadius(R.styleable.ForagePINEditText_boxCornerRadiusTopStart, boxCornerRadius)
                        val boxCornerRadiusTopEnd = typedArray.getBoxCornerRadius(R.styleable.ForagePINEditText_boxCornerRadiusTopEnd, boxCornerRadius)
                        val boxCornerRadiusBottomStart = typedArray.getBoxCornerRadius(R.styleable.ForagePINEditText_boxCornerRadiusBottomStart, boxCornerRadius)
                        val boxCornerRadiusBottomEnd = typedArray.getBoxCornerRadius(R.styleable.ForagePINEditText_boxCornerRadiusBottomEnd, boxCornerRadius)
                        val _hint = typedArray.getString(R.styleable.ForagePINEditText_hint)
                        val hintTextColor = typedArray.getColorStateList(R.styleable.ForagePINEditText_hintTextColor)
                        val inputWidth = typedArray.getDimensionPixelSize(R.styleable.ForagePINEditText_inputWidth, ViewGroup.LayoutParams.MATCH_PARENT)
                        val inputHeight = typedArray.getDimensionPixelSize(R.styleable.ForagePINEditText_inputHeight, ViewGroup.LayoutParams.WRAP_CONTENT)
                        val textSize = typedArray.getDimension(R.styleable.ForagePINEditText_textSize, -1f)
                        val textColor = typedArray.getColor(R.styleable.ForagePINEditText_textColor, Color.BLACK)

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
                            hint = _hint
                            setHintTextColor(hintTextColor)

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
                    } finally {
                        typedArray.recycle()
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

    override fun getTextElement(): EditText = _editText

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
