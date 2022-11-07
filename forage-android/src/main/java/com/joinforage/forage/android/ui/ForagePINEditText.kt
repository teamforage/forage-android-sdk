package com.joinforage.forage.android.ui

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.text.InputType
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.LinearLayout
import com.google.android.material.textfield.TextInputLayout
import com.joinforage.forage.android.R
import com.verygoodsecurity.vgscollect.widget.VGSEditText
import com.verygoodsecurity.vgscollect.widget.VGSTextInputLayout

class ForagePINEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.foragePanEditTextStyle
) : LinearLayout(context, attrs, defStyleAttr) {
    private val textInputLayout: VGSTextInputLayout
    private val textInputEditText: VGSEditText

    init {
        setWillNotDraw(false)

        orientation = VERTICAL

        context.obtainStyledAttributes(attrs, R.styleable.ForagePINEditText, defStyleAttr, 0)
            .apply {
                try {
                    val hint = getString(R.styleable.ForagePINEditText_hint)

                    val textInputLayoutStyleAttribute =
                        getResourceId(R.styleable.ForagePINEditText_pinInputLayoutStyle, 0)

                    val boxStrokeColor = getColor(
                        R.styleable.ForagePINEditText_boxStrokeColor,
                        getThemeAccentColor(context)
                    )

                    val defRadius = resources.getDimension(R.dimen.default_horizontal_field)

                    val boxCornerRadius =
                        getDimension(R.styleable.ForagePINEditText_boxCornerRadius, defRadius)

                    val boxCornerRadiusTopStart = getBoxCornerRadiusTopStart(boxCornerRadius)
                    val boxCornerRadiusTopEnd = getBoxCornerRadiusTopEnd(boxCornerRadius)
                    val boxCornerRadiusBottomStart = getBoxCornerRadiusBottomStart(boxCornerRadius)
                    val boxCornerRadiusBottomEnd = getBoxCornerRadiusBottomEnd(boxCornerRadius)

                    val hintTextColor =
                        getColorStateList(R.styleable.ForagePINEditText_hintTextColor)

                    textInputLayout =
                        VGSTextInputLayout(context, null, textInputLayoutStyleAttribute).apply {
                            layoutParams =
                                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                            setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE)

                            setHint(hint)
                            hintTextColor?.let {
                                setHintTextColor(hintTextColor)
                            }
                            setBoxStrokeColor(boxStrokeColor)

                            setBoxCornerRadius(
                                boxCornerRadiusTopStart,
                                boxCornerRadiusTopEnd,
                                boxCornerRadiusBottomStart,
                                boxCornerRadiusBottomEnd
                            )
                        }

                    val textSize = getDimension(R.styleable.ForagePINEditText_textSize, -1f)
                    val textColor = getColor(R.styleable.ForagePINEditText_textColor, Color.BLACK)

                    textInputEditText = VGSEditText(context, null, defStyleAttr).apply {
                        layoutParams =
                            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

                        setPadding(20, 20, 0, 20)

                        setSingleLine(true)
                        setFieldName("pin")
                        setText("")
                        setMaxLength(4)

                        setTextColor(textColor)
                        setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)

                        setInputType(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD)
                    }
                } finally {
                    recycle()
                }
            }

        textInputLayout.addView(textInputEditText)

        addView(textInputLayout)
    }

    private fun TypedArray.getBoxCornerRadiusBottomStart(boxCornerRadius: Float): Float {
        val boxCornerRadiusBottomStart =
            getDimension(R.styleable.ForagePINEditText_boxCornerRadiusBottomStart, 0f)
        return if (boxCornerRadiusBottomStart == 0f) boxCornerRadius else boxCornerRadiusBottomStart
    }

    private fun TypedArray.getBoxCornerRadiusTopEnd(boxCornerRadius: Float): Float {
        val boxCornerRadiusTopEnd =
            getDimension(R.styleable.ForagePINEditText_boxCornerRadiusTopEnd, 0f)
        return if (boxCornerRadiusTopEnd == 0f) boxCornerRadius else boxCornerRadiusTopEnd
    }

    private fun TypedArray.getBoxCornerRadiusBottomEnd(boxCornerRadius: Float): Float {
        val boxCornerRadiusBottomEnd =
            getDimension(R.styleable.ForagePINEditText_boxCornerRadiusBottomEnd, 0f)
        return if (boxCornerRadiusBottomEnd == 0f) boxCornerRadius else boxCornerRadiusBottomEnd
    }

    private fun TypedArray.getBoxCornerRadiusTopStart(boxCornerRadius: Float): Float {
        val boxCornerRadiusTopStart =
            getDimension(R.styleable.ForagePINEditText_boxCornerRadiusTopStart, 0f)
        return if (boxCornerRadiusTopStart == 0f) boxCornerRadius else boxCornerRadiusTopStart
    }

    internal fun getTextInputEditText(): VGSEditText {
        return textInputEditText
    }

    private fun getThemeAccentColor(context: Context): Int {
        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorAccent, outValue, true)
        return outValue.data
    }
}
