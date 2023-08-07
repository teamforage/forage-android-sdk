package com.joinforage.forage.android.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.widget.LinearLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.joinforage.forage.android.ForageSDK
import com.joinforage.forage.android.R
import com.joinforage.forage.android.core.Log
import com.joinforage.forage.android.core.element.SimpleElementListener
import com.joinforage.forage.android.core.element.StatefulElementListener
import com.joinforage.forage.android.core.element.state.ElementState
import com.joinforage.forage.android.core.element.state.PanElementStateManager
import com.joinforage.forage.android.model.PanEntry
import com.joinforage.forage.android.model.StateIIN

/**
 * Material Design component with a TextInputEditText to collect the EBT card number
 */
class ForagePANEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.foragePanEditTextStyle
) : ForageUI, LinearLayout(context, attrs, defStyleAttr), TextWatcher, ActionMode.Callback {
    private val textInputEditText: TextInputEditText
    private val textInputLayout: TextInputLayout
    private val manager: PanElementStateManager = PanElementStateManager.forEmptyInput()

    override var typeface: Typeface? = null

    init {
        // Must initialize DD at the beginning of each render function. DD requires the context,
        // so we need to wait until a context is present to run initialization code. However,
        // we have logging all over the SDK that relies on the render happening first.
        val logger = Log.getInstance()
        logger.initializeDD(context)
        setWillNotDraw(false)

        orientation = VERTICAL

        context.obtainStyledAttributes(attrs, R.styleable.ForagePANEditText, defStyleAttr, 0)
            .apply {
                try {
                    val textInputLayoutStyleAttribute =
                        getResourceId(R.styleable.ForagePANEditText_textInputLayoutStyle, 0)
                    val textColor =
                        getColor(R.styleable.ForagePANEditText_android_textColor, Color.BLACK)

                    val textSize = getDimension(R.styleable.ForagePANEditText_android_textSize, -1f)

                    textInputLayout =
                        TextInputLayout(context, null, textInputLayoutStyleAttribute).apply {
                            layoutParams =
                                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                        }

                    textInputEditText = TextInputEditText(textInputLayout.context).apply {
                        layoutParams =
                            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                        setTextIsSelectable(false)
                        inputType = InputType.TYPE_CLASS_NUMBER
                        isSingleLine = true
                        filters += InputFilter.LengthFilter(19)

                        if (textColor != Color.BLACK) {
                            setTextColor(textColor)
                        }

                        if (textSize != -1f) {
                            setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
                        }
                    }
                } finally {
                    recycle()
                }
            }

        disableCopyCardNumber()

        textInputEditText.addTextChangedListener(this)
        textInputEditText.setOnFocusChangeListener { _, hasFocus ->
            manager.changeFocus(hasFocus)
        }

        textInputLayout.addView(textInputEditText)
        textInputLayout.isErrorEnabled = true
        addView(textInputLayout)

        addView(getLogoImageViewLayout(context))
        logger.i("ForagePANEditText successfully rendered")
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
    }
    override fun setOnBlurEventListener(l: SimpleElementListener) {
        manager.setOnBlurEventListener(l)
    }
    override fun setOnChangeEventListener(l: StatefulElementListener) {
        manager.setOnChangeEventListener(l)
    }

    override fun getElementState(): ElementState {
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

    private fun disableCopyCardNumber() {
        textInputEditText.isLongClickable = false
        textInputEditText.customSelectionActionModeCallback = this
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(cardNumber: CharSequence?, start: Int, before: Int, count: Int) {
        manager.handleChangeEvent(cardNumber.toString())
    }

    override fun afterTextChanged(s: Editable?) {
        val input = s.toString()
        if (isNumeric(input)) {
            val stateInnOrNull = StateIIN.values()
                .find { input.startsWith(it.iin) && input.length == it.panLength }

            if (stateInnOrNull == null) {
                textInputLayout.error = context.getString(R.string.ebt_card_validation_error)
                ForageSDK.storeEntry(PanEntry.Invalid(input))
            } else {
                textInputLayout.error = null
                ForageSDK.storeEntry(PanEntry.Valid(input))
            }
        } else {
            textInputLayout.error = context.getString(R.string.ebt_card_validation_error)
            ForageSDK.storeEntry(PanEntry.Invalid(input))
        }
    }

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return false
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return false
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        return false
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
    }

    private fun isNumeric(input: String) = input.matches("[0-9]+".toRegex())
}
