package com.joinforage.forage.android.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.InputType
import android.text.method.DigitsKeyListener
import android.util.AttributeSet
import android.util.TypedValue
import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.joinforage.forage.android.ForageConfigNotSetException
import com.joinforage.forage.android.R
import com.joinforage.forage.android.core.EnvConfig
import com.joinforage.forage.android.core.element.SimpleElementListener
import com.joinforage.forage.android.core.element.StatefulElementListener
import com.joinforage.forage.android.core.element.state.PanElementState
import com.joinforage.forage.android.core.element.state.PanElementStateManager
import com.joinforage.forage.android.core.telemetry.Log

/**
 * A [ForageElement] that securely collects a customer's card number. You need a [ForagePANEditText]
 * to call the ForageSDK online-only method to
 * [tokenize an EBT Card][com.joinforage.forage.android.ForageSDK.tokenizeEBTCard], or
 * the ForageTerminalSDK POS method to
 * [tokenize a card][com.joinforage.forage.android.pos.ForageTerminalSDK.tokenizeCard].
 * ```xml
 * <!-- Example forage_pan_component.xml -->
 * <?xml version="1.0" encoding="utf-8"?>
 * <androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
 *     xmlns:app="http://schemas.android.com/apk/res-auto"
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent">
 *
 *     <com.joinforage.forage.android.ui.ForagePANEditText
 *             android:id="@+id/foragePanEditText"
 *             android:layout_width="0dp"
 *             android:layout_height="wrap_content"
 *             android:layout_margin="16dp"
 *             app:layout_constraintBottom_toBottomOf="parent"
 *             app:layout_constraintEnd_toEndOf="parent"
 *             app:layout_constraintStart_toStartOf="parent"
 *             app:layout_constraintTop_toTopOf="parent"
 *     />
 *
 * </androidx.constraintlayout.widget.ConstraintLayout>
 * ```
 * @see * [Guide to styling Forage Android Elements](https://docs.joinforage.app/docs/forage-android-styling-guide)
 * * [Online-only Android Quickstart](https://docs.joinforage.app/docs/forage-android-quickstart)
 * * [POS Terminal Android Quickstart](https://docs.joinforage.app/docs/forage-terminal-android)
 */
class ForagePANEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.foragePanEditTextStyle
) : AbstractForageElement<PanElementState>(context, attrs, defStyleAttr) {
    private val textInputEditText: TextInputEditText
    private val textInputLayout: TextInputLayout

    /**
     * The `manager` property acts as an abstraction for the actual code
     * in ForagePANEditText, allowing it to work with a non-nullable
     * result determined by the choice between `prod` or `sandbox`.
     * This choice requires knowledge of the environment,which is determined
     * by `forageConfig` set on this instance.
     *
     * The underlying value for `manager` is stored in `_SET_ONLY_manager`.
     * This backing property is set only after `ForageConfig` has been
     * initialized for this instance. If `manager` is accessed before
     * `_SET_ONLY_manager` is set, a runtime exception is thrown.
     */
    private var _SET_ONLY_manager: PanElementStateManager? = null
    private val manager: PanElementStateManager
        get() {
            if (_SET_ONLY_manager == null) {
                throw ForageConfigNotSetException(
                    """You are attempting invoke a method a ForageElement before setting
                    it's ForageConfig. Make sure to call
                    myForageElement.setForageConfig(forageConfig: ForageConfig) 
                    immediately on your ForageElement before you call any other methods.
                    """.trimIndent()
                )
            }
            return _SET_ONLY_manager!!
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

    override fun showKeyboard() {
        val imm = context.getSystemService<InputMethodManager>()
        imm!!.showSoftInput(textInputEditText, 0)
    }

    override fun initWithForageConfig(forageConfig: ForageConfig, isPos: Boolean) {
        // Must initialize DD at the beginning of each render function. DD requires the context,
        // so we need to wait until a context is present to run initialization code. However,
        // we have logging all over the SDK that relies on the render happening first.
        val logger = Log.getInstance()
        logger.initializeDD(context, forageConfig)

        _SET_ONLY_manager = if (EnvConfig.inProd(forageConfig)) {
            // strictly support only valid Ebt PAN numbers
            PanElementStateManager.forEmptyInput()
        } else {
            // allows whitelist of special Ebt PAN numbers
            PanElementStateManager.NON_PROD_forEmptyInput()
        }

        // register FormatPanTextWatcher to keep the format up to date
        // with each user input based on the StateIIN
        // Since the FormatPanTextWatcher knows which input changes
        // are actually the users and which ones are programmatic updates
        // for reformatting purposes, we'll let the FormatPanTextWatcher
        // decide when its appropriate to invoke the user-registered callback
        val formatPanTextWatcher = FormatPanTextWatcher(textInputEditText)
        formatPanTextWatcher.onFormattedChangeEvent { formattedCardNumber ->
            manager.handleChangeEvent(formattedCardNumber)
        }
        textInputEditText.addTextChangedListener(formatPanTextWatcher)

        textInputLayout.addView(textInputEditText)
        addView(textInputLayout)

        addView(getLogoImageViewLayout(context))
        logger.i("[View] ForagePANEditText successfully rendered")
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
            manager.changeFocus(hasFocus)
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
        manager.setOnFocusEventListener(l)
        restartFocusChangeListener()
    }
    override fun setOnBlurEventListener(l: SimpleElementListener) {
        manager.setOnBlurEventListener(l)
        restartFocusChangeListener()
    }
    override fun setOnChangeEventListener(l: StatefulElementListener<PanElementState>) {
        manager.setOnChangeEventListener(l)
    }

    override fun getElementState(): PanElementState {
        return manager.getState()
    }

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
