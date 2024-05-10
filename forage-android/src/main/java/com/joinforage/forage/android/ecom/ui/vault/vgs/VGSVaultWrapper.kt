package com.joinforage.forage.android.ecom.ui.vault.vgs

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import com.joinforage.forage.android.core.services.ForageConstants
import com.joinforage.forage.android.core.services.VaultType
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.services.vault.AbstractVaultSubmitter
import com.joinforage.forage.android.core.ui.VaultWrapper
import com.joinforage.forage.android.core.ui.element.ForagePinElement
import com.joinforage.forage.android.core.ui.element.state.PinElementStateManager
import com.joinforage.forage.android.core.ui.getBoxCornerRadiusBottomEnd
import com.joinforage.forage.android.core.ui.getBoxCornerRadiusBottomStart
import com.joinforage.forage.android.core.ui.getBoxCornerRadiusTopEnd
import com.joinforage.forage.android.core.ui.getBoxCornerRadiusTopStart
import com.joinforage.forage.android.ecom.services.vault.vgs.VgsPinSubmitter
import com.verygoodsecurity.vgscollect.core.model.state.FieldState
import com.verygoodsecurity.vgscollect.core.storage.OnFieldStateChangeListener
import com.verygoodsecurity.vgscollect.view.card.validation.rules.VGSInfoRule
import com.verygoodsecurity.vgscollect.widget.VGSEditText

internal class VGSVaultWrapper @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VaultWrapper(context, attrs, defStyleAttr) {
    private var _internalEditText: VGSEditText
    override val manager: PinElementStateManager = PinElementStateManager.forEmptyInput()
    override val vaultType: VaultType = VaultType.VGS_VAULT_TYPE

    init {
        context.obtainStyledAttributes(attrs, com.joinforage.forage.android.R.styleable.ForagePINEditText, defStyleAttr, 0)
            .apply {
                try {
                    val hint = getString(com.joinforage.forage.android.R.styleable.ForagePINEditText_hint)
                    val textInputLayoutStyleAttribute =
                        getResourceId(com.joinforage.forage.android.R.styleable.ForagePINEditText_pinInputLayoutStyle, 0)
                    val boxStrokeColor = getColor(
                        com.joinforage.forage.android.R.styleable.ForagePINEditText_pinBoxStrokeColor,
                        getThemeAccentColor(context)
                    )
                    val boxBackgroundColor = getColor(com.joinforage.forage.android.R.styleable.ForagePINEditText_boxBackgroundColor, Color.TRANSPARENT)
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

                    val inputWidth: Int = getDimensionPixelSize(com.joinforage.forage.android.R.styleable.ForagePINEditText_inputWidth, ViewGroup.LayoutParams.MATCH_PARENT)
                    val inputHeight: Int = getDimensionPixelSize(com.joinforage.forage.android.R.styleable.ForagePINEditText_inputHeight, ViewGroup.LayoutParams.WRAP_CONTENT)

                    _internalEditText = VGSEditText(context, null, textInputLayoutStyleAttribute).apply {
                        layoutParams =
                            LinearLayout.LayoutParams(
                                inputWidth,
                                inputHeight
                            )

                        setHint(hint)
                        hintTextColor?.let {
                            setHintTextColor(hintTextColor)
                        }

                        val customBackground = GradientDrawable().apply {
                            setPaddingRelative(20, 20, 20, 20)
                            shape = GradientDrawable.RECTANGLE
                            cornerRadii = floatArrayOf(boxCornerRadiusTopStart, boxCornerRadiusTopStart, boxCornerRadiusTopEnd, boxCornerRadiusTopEnd, boxCornerRadiusBottomStart, boxCornerRadiusBottomStart, boxCornerRadiusBottomEnd, boxCornerRadiusBottomEnd)
                            setStroke(5, boxStrokeColor)
                            setColor(boxBackgroundColor)
                        }
                        background = customBackground

                        setTextColor(textColor)
                        setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
                        setGravity(Gravity.CENTER)

                        // Constant VGS configuration. Can't be externally altered!
                        setFieldName(ForageConstants.VGS.PIN_FIELD_NAME)
                        setMaxLength(4)
                        setInputType(android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD)
                        setPadding(20, 20, 20, 20)
                    }
                    // enforce that PINs must be 4 digits to be vali
                    _internalEditText.appendRule(
                        VGSInfoRule.ValidationBuilder()
                            .setRegex("\\d{4}")
                            .build()
                    )

                    // VGS works with the conventional setOnFocusChangeListener
                    // see https://tinyurl.com/2urct5er, which means a single
                    // listener handles the focus and blur logic. We split this
                    // up into separate focus and blur listeners. This requires
                    // that we pass a single listener to VGS on init that uses
                    // mutable references to listeners so that setting the focus
                    // would not remove the blur listener and vice versa
                    _internalEditText.setOnFocusChangeListener { _, hasFocus ->
                        manager.changeFocus(hasFocus)
                    }
                    _internalEditText.setOnFieldStateChangeListener(object : OnFieldStateChangeListener {
                        override fun onStateChange(state: FieldState) {
                            // map VGS's event representation to Forage's
                            manager.handleChangeEvent(
                                isComplete = state.isValid,
                                isEmpty = state.isEmpty
                            )
                        }
                    })
                } finally {
                    recycle()
                }
            }
    }

    override fun clearText() {
        _internalEditText.setText("")
    }

    override fun getTextElement(): VGSEditText = _internalEditText

    override fun getVaultSubmitter(
        foragePinElement: ForagePinElement,
        logger: Log
    ): AbstractVaultSubmitter = VgsPinSubmitter(
        foragePinElement.context,
        foragePinElement,
        logger
    )

    override fun showKeyboard() {
        _internalEditText.showKeyboard()
    }

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
}
