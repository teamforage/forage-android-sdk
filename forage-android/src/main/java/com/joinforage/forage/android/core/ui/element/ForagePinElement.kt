package com.joinforage.forage.android.core.ui.element

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
import com.joinforage.forage.android.core.services.forageapi.engine.IHttpEngine
import com.joinforage.forage.android.core.services.vault.ISecurePinCollector
import com.joinforage.forage.android.core.services.vault.RosettaPinSubmitter
import com.joinforage.forage.android.core.ui.element.state.FocusState
import com.joinforage.forage.android.core.ui.element.state.pin.PinEditTextState
import com.joinforage.forage.android.core.ui.element.state.pin.PinInputState
import com.joinforage.forage.android.core.ui.getBoxCornerRadiusBottomEnd
import com.joinforage.forage.android.core.ui.getBoxCornerRadiusBottomStart
import com.joinforage.forage.android.core.ui.getBoxCornerRadiusTopEnd
import com.joinforage.forage.android.core.ui.getBoxCornerRadiusTopStart
import com.joinforage.forage.android.core.ui.textwatcher.PinTextWatcher

/**
 * A [ForageElement] that securely collects a card PIN. You can use an instance of a [ForagePinElement]
 * to call the methods that:
 * * [Check a card's balance][com.joinforage.forage.android.pos.services.ForageTerminalSDK.checkBalance]
 * * [Collect a card PIN to defer payment capture to the server][com.joinforage.forage.android.pos.services.ForageTerminalSDK.deferPaymentCapture]
 * * [Capture a payment immediately][com.joinforage.forage.android.pos.services.ForageTerminalSDK.capturePayment]
 * * [Refund a Payment immediately][com.joinforage.forage.android.pos.services.ForageTerminalSDK.refundPayment]
 * * [Collect a card PIN to defer payment refund to the server][com.joinforage.forage.android.pos.services.ForageTerminalSDK.deferPaymentRefund]
 */
abstract class ForagePinElement @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.foragePanEditTextStyle
) : ForageVaultElement<PinEditTextState>(context, attrs, defStyleAttr), EditTextElement {
    private val _linearLayout: LinearLayout
    internal val _editText: EditText

    private var focusState = FocusState.forEmptyInput()
    private var inputState = PinInputState.forEmptyInput()
    val pinEditTextState: PinEditTextState
        get() = PinEditTextState.from(focusState, inputState)

    // mutable references to event listeners. We use mutable
    // references because the implementations of our vaults
    // require that we are only able to ever pass a single
    // monolithic event within init call. This is mutability
    // allows us simulate setting and overwriting a listener
    // with every set call
    private var onFocusEventListener: SimpleElementListener? = null
    private var onBlurEventListener: SimpleElementListener? = null
    private var onChangeEventListener: StatefulElementListener<PinEditTextState>? = null

    init {
        context.obtainStyledAttributes(attrs, R.styleable.ForagePINEditText, defStyleAttr, 0)
            .apply {
                try {
                    setWillNotDraw(false)
                    orientation = VERTICAL
                    gravity = Gravity.CENTER

                    val elementWidth: Int = getDimensionPixelSize(R.styleable.ForagePINEditText_elementWidth, ViewGroup.LayoutParams.MATCH_PARENT)
                    val elementHeight: Int = getDimensionPixelSize(R.styleable.ForagePINEditText_elementHeight, ViewGroup.LayoutParams.WRAP_CONTENT)

                    _linearLayout = LinearLayout(context)
                    _linearLayout.layoutParams = ViewGroup.LayoutParams(elementWidth, elementHeight)
                    _linearLayout.orientation = VERTICAL
                    _linearLayout.gravity = Gravity.CENTER

                    _editText = buildEditText(attrs, defStyleAttr)
                    registerEventListeners()
                } finally {
                    recycle()
                }
            }
    }

    private fun getThemeAccentColor(context: Context): Int {
        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorAccent, outValue, true)
        return outValue.data
    }

    private fun buildEditText(attrs: AttributeSet? = null, defStyleAttr: Int): EditText {
        val defaultRadius = resources.getDimension(R.dimen.default_horizontal_field)
        val typedArray: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.ForagePINEditText, defStyleAttr, 0)
        val boxCornerRadius = typedArray.getDimension(R.styleable.ForagePINEditText_boxCornerRadius, defaultRadius)

        try {
            val textInputLayoutStyleAttribute =
                typedArray.getResourceId(
                    R.styleable.ForagePINEditText_pinInputLayoutStyle,
                    0
                )
            val boxStrokeColor = typedArray.getColor(
                R.styleable.ForagePINEditText_pinBoxStrokeColor,
                getThemeAccentColor(context)
            )
            val boxBackgroundColor = typedArray.getColor(
                R.styleable.ForagePINEditText_boxBackgroundColor,
                Color.TRANSPARENT
            )
            // getBoxCornerRadius*** methods use the ForagePANEditText
            // (instead of the ForagePINEditText) styling options
            // This will be fixed in the future major version of the Android SDK
            val boxCornerRadiusTopStart = typedArray.getBoxCornerRadiusTopStart(boxCornerRadius)
            val boxCornerRadiusTopEnd = typedArray.getBoxCornerRadiusTopEnd(boxCornerRadius)
            val boxCornerRadiusBottomStart = typedArray.getBoxCornerRadiusBottomStart(boxCornerRadius)
            val boxCornerRadiusBottomEnd = typedArray.getBoxCornerRadiusBottomEnd(boxCornerRadius)
            val _hint = typedArray.getString(R.styleable.ForagePINEditText_hint)
            val hintTextColor =
                typedArray.getColorStateList(R.styleable.ForagePINEditText_hintTextColor)
            val inputWidth = typedArray.getDimensionPixelSize(
                R.styleable.ForagePINEditText_inputWidth,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            val inputHeight = typedArray.getDimensionPixelSize(
                R.styleable.ForagePINEditText_inputHeight,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            val textSize =
                typedArray.getDimension(R.styleable.ForagePINEditText_textSize, -1f)
            val textColor =
                typedArray.getColor(R.styleable.ForagePINEditText_textColor, Color.BLACK)

            return EditText(context, null, textInputLayoutStyleAttribute).apply {
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
    }

    private fun registerEventListeners() {
        registerFocusChangeListener()
        registerTextWatcher()
    }

    private fun registerFocusChangeListener() {
        _editText.setOnFocusChangeListener { _, hasFocus ->
            focusState = focusState.changeFocus(hasFocus)
            focusState.fireEvent(
                onFocusEventListener = onFocusEventListener,
                onBlurEventListener = onBlurEventListener
            )
        }
    }

    private fun registerTextWatcher() {
        val pinTextWatcher = PinTextWatcher()
        pinTextWatcher.onInputChangeEvent { isComplete, isEmpty ->
            inputState = inputState.handleChangeEvent(
                isComplete = isComplete,
                isEmpty = isEmpty
            )
            onChangeEventListener?.invoke(pinEditTextState)
        }
        _editText.addTextChangedListener(pinTextWatcher)
    }

    override fun clearText() {
        _editText.setText("")
    }

    override fun showKeyboard() {
        val imm = context.getSystemService<InputMethodManager>()
        imm!!.showSoftInput(_editText, 0)
    }

    override fun getVaultSubmitter(
        envConfig: EnvConfig,
        httpEngine: IHttpEngine
    ): RosettaPinSubmitter = RosettaPinSubmitter(
        _editText.text.toString(),
        object : ISecurePinCollector {
            override fun clearText() {
                this@ForagePinElement.clearText()
            }
            override fun isComplete(): Boolean = inputState.isComplete
        },
        httpEngine
    )

    // While the events that ForageElements expose mirrors the
    // blur, focus, change etc events of an Android view,
    // they represent different abstractions. Our users need to
    // interact with the ForageElement abstraction and not the
    // implementation details of which Android view we use.
    // Therefore we expose novel set listener methods instead of
    // overriding the convention setOn*Listener
    override fun setOnFocusEventListener(l: SimpleElementListener) {
        onFocusEventListener = l
    }
    override fun setOnBlurEventListener(l: SimpleElementListener) {
        onBlurEventListener = l
    }
    override fun setOnChangeEventListener(l: StatefulElementListener<PinEditTextState>) {
        onChangeEventListener = l
    }

    override fun getElementState(): PinEditTextState = pinEditTextState

    override fun setTextColor(textColor: Int) {
        _editText.setTextColor(textColor)
    }

    override fun setTextSize(textSize: Float) {
        _editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
    }

    override fun setBoxStrokeColor(boxStrokeColor: Int) {
        // no-ops for now
    }
    override fun setBoxStrokeWidth(boxStrokeWidth: Int) {
        // no-ops for now
    }
    override fun setBoxStrokeWidthFocused(boxStrokeWidth: Int) {
        // no-ops for now
    }

    override var typeface: Typeface?
        get() = _editText.typeface
        set(value) {
            if (value != null) {
                _editText.typeface = value
            }
        }

    @Deprecated(
        message = "setHint (for *PIN* elements) is deprecated.",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("")
    )
    /**
     * @deprecated setHint (for **PIN** elements) is deprecated.
     */
    override fun setHint(hint: String) {
        // no-op, deprecated!
    }

    @Deprecated(
        message = "setHintTextColor (for *PIN* elements) is deprecated.",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("")
    )
    /**
     * @deprecated setHintTextColor (for **PIN** elements) is deprecated.
     */
    override fun setHintTextColor(hintTextColor: Int) {
        // no-op, deprecated!
    }
}
