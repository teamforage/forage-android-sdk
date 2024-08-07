package com.joinforage.forage.android.core.ui.element

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.InputType
import android.text.method.DigitsKeyListener
import android.util.AttributeSet
import android.util.TypedValue
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.core.content.getSystemService
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.joinforage.forage.android.R
import com.joinforage.forage.android.core.services.EnvConfig
import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.ForageConfigNotSetException
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.ui.element.state.FocusState
import com.joinforage.forage.android.core.ui.element.state.pan.PanEditTextState
import com.joinforage.forage.android.core.ui.element.state.pan.PanInputState
import com.joinforage.forage.android.core.ui.getBoxCornerRadiusBottomEnd
import com.joinforage.forage.android.core.ui.getBoxCornerRadiusBottomStart
import com.joinforage.forage.android.core.ui.getBoxCornerRadiusTopEnd
import com.joinforage.forage.android.core.ui.getBoxCornerRadiusTopStart
import com.joinforage.forage.android.core.ui.getLogoImageViewLayout
import com.joinforage.forage.android.core.ui.getThemeAccentColor
import com.joinforage.forage.android.core.ui.textwatcher.FormatPanTextWatcher

/**
 * A [ForageElement] that securely collects a customer's card number. You need an instance of a
 * [ForagePanElement] to call the method to manually [tokenize a card][com.joinforage.forage.android.pos.services.ForageTerminalSDK.tokenizeCard].
 */
abstract class ForagePanElement @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.foragePanEditTextStyle
) : LinearLayout(context, attrs, defStyleAttr), ForageElement<PanEditTextState>, EditTextElement, DynamicEnvElement {
    private val textInputEditText: TextInputEditText
    private val textInputLayout: TextInputLayout

    private var onFocusEventListener: SimpleElementListener? = null
    private var onBlurEventListener: SimpleElementListener? = null
    private var onChangeEventListener: StatefulElementListener<PanEditTextState>? = null
    private var focusState = FocusState.forEmptyInput()

    /**
     * The `inputState` property acts as an abstraction for the actual code
     * in ForagePANEditText, allowing it to work with a non-nullable
     * result determined by the choice between `prod` or `sandbox`.
     * This choice requires knowledge of the environment,which is determined
     * by `forageConfig` set on this instance.
     *
     * The underlying value for `inputState` is stored in
     * `_SET_ONLY_inputState`. This backing property is set only after
     * `ForageConfig` has been initialized for this instance. If `manager`
     * is accessed before `_SET_ONLY_inputState` is set, a runtime exception
     * is thrown.
     */
    private var _SET_ONLY_inputState: PanInputState? = null
    private val inputState: PanInputState
        get() {
            if (_SET_ONLY_inputState == null) {
                throw ForageConfigNotSetException(
                    """You are attempting invoke a method a ForageElement before setting
                    it's ForageConfig. Make sure to call
                    myForageElement.setForageConfig(forageConfig: ForageConfig) 
                    immediately on your ForageElement before you call any other methods.
                    """.trimIndent()
                )
            }
            return _SET_ONLY_inputState!!
        }

    override var typeface: Typeface? = null

    init {
        setWillNotDraw(false)

        orientation = VERTICAL

        context.obtainStyledAttributes(attrs, R.styleable.ForagePANEditText, defStyleAttr, 0)
            .apply {
                try {
                    val panBoxStrokeColor = getColor(
                        R.styleable.ForagePANEditText_panBoxStrokeColor,
                        getThemeAccentColor(context)
                    )

                    val panBoxStrokeWidthFocused = getDimension(
                        R.styleable.ForagePANEditText_panBoxStrokeWidthFocused,
                        3f
                    )

                    val panBoxStrokeWidth = getDimension(
                        R.styleable.ForagePANEditText_panBoxStrokeWidth,
                        3f
                    )

                    val textInputLayoutStyleAttribute =
                        getResourceId(R.styleable.ForagePANEditText_textInputLayoutStyle, 0)
                    val textColor =
                        getColor(R.styleable.ForagePANEditText_android_textColor, Color.BLACK)

                    val textSize = getDimension(R.styleable.ForagePANEditText_android_textSize, -1f)

                    val defRadius = resources.getDimension(R.dimen.default_horizontal_field)
                    val focusedBoxCornerRadius =
                        getDimension(R.styleable.ForagePANEditText_cornerRadius, defRadius)

                    val boxCornerRadiusTopStart = getBoxCornerRadiusTopStart(focusedBoxCornerRadius)
                    val boxCornerRadiusTopEnd = getBoxCornerRadiusTopEnd(focusedBoxCornerRadius)
                    val boxCornerRadiusBottomStart = getBoxCornerRadiusBottomStart(focusedBoxCornerRadius)
                    val boxCornerRadiusBottomEnd = getBoxCornerRadiusBottomEnd(focusedBoxCornerRadius)

                    textInputLayout =
                        TextInputLayout(context, null, textInputLayoutStyleAttribute).apply {
                            layoutParams =
                                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

                            // Set stroke color on focused text
                            boxStrokeColor = panBoxStrokeColor
                            // Set stroke width on focused text
                            boxStrokeWidthFocused = panBoxStrokeWidthFocused.toInt()
                            // Set stroke width on unfocused text
                            boxStrokeWidth = panBoxStrokeWidth.toInt()

                            setBoxCornerRadii(
                                boxCornerRadiusTopStart,
                                boxCornerRadiusTopEnd,
                                boxCornerRadiusBottomStart,
                                boxCornerRadiusBottomEnd
                            )
                        }

                    textInputEditText = TextInputEditText(textInputLayout.context).apply {
                        layoutParams =
                            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                        setTextIsSelectable(false)
                        isSingleLine = true

                        if (textColor != Color.BLACK) {
                            setTextColor(textColor)
                        }

                        if (textSize != -1f) {
                            setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
                        }

                        // make the keyboard digits only instead of QWERTY. It is
                        // necessary to declare that we accept digits and " " so
                        // that the app does not crash when the PanFormatTextWatcher
                        // programmatically inserts spaces. By default it disallows
                        // whitespace
                        inputType = InputType.TYPE_CLASS_NUMBER
                        keyListener = DigitsKeyListener.getInstance("0123456789 ")
                    }
                } finally {
                    recycle()
                }
            }
    }

    private val forageConfigManager = ForageConfigManager {
            forageConfig ->
        initWithForageConfig(forageConfig)
    }

    /**
     * Sets the necessary [ForageConfig] configuration properties for a [ForagePanElement].
     * **[setForageConfig] must be called before any other methods can be executed on
     * the ForagePanElement.**
     * ```kotlin
     * // Example: Call setForageConfig on a ForagePANEditText Element
     * val foragePanEditText = root?.findViewById<ForagePANEditText>(
     *     R.id.tokenizeForagePanEditText
     * )
     * foragePanEditText.setForageConfig(
     *     ForageConfig(
     *         merchantId = "<merchant_id>",
     *         sessionToken = "<session_token>"
     *     )
     * )
     * ```
     *
     * @param forageConfig A [ForageConfig] instance that specifies a `merchantId` and `sessionToken`.
     */
    override fun setForageConfig(forageConfig: ForageConfig) {
        forageConfigManager.forageConfig = forageConfig
    }

    internal fun getForageConfig() = forageConfigManager.forageConfig

    private fun initWithForageConfig(forageConfig: ForageConfig) {
        // Must initialize DD at the beginning of each render function. DD requires the context,
        // so we need to wait until a context is present to run initialization code. However,
        // we have logging all over the SDK that relies on the render happening first.
        val logger = Log.getInstance()
        logger.initializeDD(context, forageConfig)

        _SET_ONLY_inputState = if (EnvConfig.inProd(forageConfig)) {
            // strictly support only valid Ebt PAN numbers
            PanInputState.forEmptyInput()
        } else {
            // allows whitelist of special Ebt PAN numbers
            PanInputState.NON_PROD_forEmptyInput()
        }

        // register FormatPanTextWatcher to keep the format up to date
        // with each user input based on the StateIIN
        // Since the FormatPanTextWatcher knows which input changes
        // are actually the users and which ones are programmatic updates
        // for reformatting purposes, we'll let the FormatPanTextWatcher
        // decide when its appropriate to invoke the user-registered callback
        val formatPanTextWatcher = FormatPanTextWatcher(textInputEditText)
        formatPanTextWatcher.onFormattedChangeEvent { formattedCardNumber ->
            _SET_ONLY_inputState = inputState.handleChangeEvent(formattedCardNumber)
            onChangeEventListener?.invoke(getElementState())
        }
        textInputEditText.addTextChangedListener(formatPanTextWatcher)

        textInputLayout.addView(textInputEditText)
        addView(textInputLayout)

        addView(getLogoImageViewLayout(context))
        logger.i("[View] ForagePANEditText successfully rendered")
    }

    override fun showKeyboard() {
        val imm = context.getSystemService<InputMethodManager>()
        imm!!.showSoftInput(textInputEditText, 0)
    }

    override fun clearText() {
        textInputEditText.setText("")
    }

    // NOTE: do not call this method inside `init {}`
    // There was a timing bug that caused the focus
    // callback to not get registered correctly when
    // called from `init {}`. Calling it afterwards
    // seems to resolve the issue
    // https://joinforage.slack.com/archives/C04FQM5F2DA/p1691443021122609
    private fun restartFocusChangeListener() {
        // this is an idempotent operation because overwriting
        // the previous callback with the same callback over and
        // over will continue to correctly call the developer's
        // focus/blur events
        textInputEditText.setOnFocusChangeListener { _, hasFocus ->
            focusState = focusState.changeFocus(hasFocus)
            focusState.fireEvent(
                onFocusEventListener = onFocusEventListener,
                onBlurEventListener = onBlurEventListener
            )
        }
    }

    // While the events that ForageElements expose mirrors the
    // blur, focus, change etc events of an Android view,
    // they represent different abstractions. Our users need to
    // interact with the ForageElement abstraction and not the
    // implementation details of which Android view we use.
    // Therefore we expose novel set listener methods instead of
    // overriding the convention setOn*Listener
    override fun setOnFocusEventListener(l: SimpleElementListener) {
        onFocusEventListener = l
        restartFocusChangeListener()
    }
    override fun setOnBlurEventListener(l: SimpleElementListener) {
        onBlurEventListener = l
        restartFocusChangeListener()
    }
    override fun setOnChangeEventListener(l: StatefulElementListener<PanEditTextState>) {
        onChangeEventListener = l
    }

    override fun getElementState(): PanEditTextState = PanEditTextState.from(
        focusState,
        inputState
    )

    override fun setTextColor(textColor: Int) {
        // no-ops for now
    }
    override fun setTextSize(textSize: Float) {
        // no-ops for now
    }
    override fun setHint(hint: String) {
        // no-ops for now
    }
    override fun setHintTextColor(hintTextColor: Int) {
        // no-ops for now
    }
    override fun setBoxStrokeColor(boxStrokeColor: Int) {
        textInputLayout.boxStrokeColor = boxStrokeColor
    }

    override fun setBoxStrokeWidth(boxStrokeWidth: Int) {
        textInputLayout.boxStrokeWidth = boxStrokeWidth
    }
    override fun setBoxStrokeWidthFocused(boxStrokeWidth: Int) {
        textInputLayout.boxStrokeWidthFocused = boxStrokeWidth
    }

    internal fun getPanNumber(): String {
        val rawText = textInputEditText.text.toString()

        // remove format spacing
        return rawText.filter { it.isDigit() }
    }
}
