package com.joinforage.forage.android.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.LinearLayout
import com.basistheory.android.view.TextElement
import com.joinforage.forage.android.network.ForageConstants
import com.verygoodsecurity.vgscollect.widget.VGSEditText

internal class VGSVaultWrapper @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VaultWrapper(context, attrs, defStyleAttr) {
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

    override var typeface: Typeface?
        get() = _internalEditText.getTypeface()
        set(value) {
            if (value != null) {
                _internalEditText.setTypeface(value)
            }
        }

    override fun setTextColor(textColor: Int) {
        _internalEditText.setTextColor(textColor)
    }

    override fun setTextSize(textSize: Float) {
        _internalEditText.setTextSize(textSize)
    }

    override fun setHint(hint: String) {
        _internalEditText.setHint(hint)
    }

    override fun setHintTextColor(hintTextColor: Int) {
        _internalEditText.setHintTextColor(hintTextColor)
    }

    override val hasFocus: Boolean = _internalEditText.hasFocus()
}
