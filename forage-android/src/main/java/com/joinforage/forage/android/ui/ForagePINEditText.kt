package com.joinforage.forage.android.ui

import android.app.Application
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.LinearLayout
import com.basistheory.android.service.BasisTheoryElements
import com.basistheory.android.view.TextElement
import com.basistheory.android.view.mask.ElementMask
import com.basistheory.android.view.validation.RegexValidator
import com.google.android.material.textfield.TextInputLayout
import com.joinforage.forage.android.LDManager
import com.joinforage.forage.android.R
import com.joinforage.forage.android.VaultConstants
import com.joinforage.forage.android.collect.BTPinCollector
import com.joinforage.forage.android.collect.PinCollector
import com.joinforage.forage.android.collect.VGSPinCollector
import com.joinforage.forage.android.network.ForageConstants
import com.verygoodsecurity.vgscollect.widget.VGSEditText
import com.verygoodsecurity.vgscollect.widget.VGSTextInputLayout

/**
 * Material component using VGSTextInputLayout and VGSEditText to collect the PIN
 */
class ForagePINEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.foragePanEditTextStyle
) : LinearLayout(context, attrs, defStyleAttr) {
    private var textInputLayout: VGSTextInputLayout? = null
    private var textInputEditText: VGSEditText? = null

    private var btTextInput: TextElement? = null
    private var vaultType: String

    init {
        setWillNotDraw(false)

        orientation = VERTICAL

        vaultType = LDManager.getVaultProvider(context.applicationContext as Application)
        if (vaultType == VaultConstants.BT_VAULT_TYPE) {
            renderBt(context, attrs, defStyleAttr)
        } else if (vaultType == VaultConstants.VGS_VAULT_TYPE) {
            renderVgs(context, attrs, defStyleAttr)
        } else {
            throw Error("This shouldn't be possible!!")
        }
    }

    // TODO: Ask BT to add other styling values or use the subset of styling here.
    private fun renderBt(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = R.attr.foragePanEditTextStyle
    ) {
        context.obtainStyledAttributes(attrs, R.styleable.ForagePINEditText, defStyleAttr, 0)
            .apply {
                try {
                    val textInputLayoutStyleAttribute =
                        getResourceId(R.styleable.ForagePINEditText_pinInputLayoutStyle, 0)

                    btTextInput = TextElement(context, null, textInputLayoutStyleAttribute).apply {
                        layoutParams =
                            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                        inputType = com.basistheory.android.model.InputType.NUMBER_PASSWORD
                        val digit = Regex("""\d""")
                        mask = ElementMask(listOf(digit, digit, digit, digit))
                        hint = getString(R.styleable.ForagePINEditText_hint)
                        textSize = getDimension(R.styleable.ForagePINEditText_textSize, -1f)
                        textColor = getColor(R.styleable.ForagePINEditText_textColor, Color.BLACK)
                    }
                } finally {
                    recycle()
                }
            }

        addView(btTextInput)
        addView(getLogoImageViewLayout(context))
    }

    private fun renderVgs(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = R.attr.foragePanEditTextStyle
    ) {
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
                        setFieldName(ForageConstants.VGS.PIN_FIELD_NAME)
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

        textInputLayout?.addView(textInputEditText)

        addView(textInputLayout)

        addView(getLogoImageViewLayout(context))
    }

    internal fun getVaultType(): String {
        return vaultType
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

    internal fun getCollector(
        merchantAccount: String
    ): PinCollector {
        if (vaultType == VaultConstants.BT_VAULT_TYPE) {
            return BTPinCollector(
                this,
                merchantAccount
            )
        }
        return VGSPinCollector(
            context,
            this,
            merchantAccount
        )
    }

    internal fun getTextInputEditText(): VGSEditText {
        return textInputEditText as VGSEditText
    }

    internal fun getTextElement(): TextElement {
        return btTextInput as TextElement
    }

    private fun getThemeAccentColor(context: Context): Int {
        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorAccent, outValue, true)
        return outValue.data
    }
}
