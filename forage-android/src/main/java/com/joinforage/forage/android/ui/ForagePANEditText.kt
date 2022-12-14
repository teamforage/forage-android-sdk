package com.joinforage.forage.android.ui

import android.content.Context
import android.graphics.Color
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
import com.joinforage.forage.android.model.PanEntry
import com.joinforage.forage.android.model.StateIIN

/**
 * Material Design component with a TextInputEditText to collect the EBT card number
 */
class ForagePANEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.foragePanEditTextStyle
) : LinearLayout(context, attrs, defStyleAttr), TextWatcher, ActionMode.Callback {
    private val textInputEditText: TextInputEditText
    private val textInputLayout: TextInputLayout

    init {
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

        textInputLayout.addView(textInputEditText)
        textInputLayout.isErrorEnabled = true
        addView(textInputLayout)

        addView(getLogoImageViewLayout(context))
    }

    private fun disableCopyCardNumber() {
        textInputEditText.isLongClickable = false
        textInputEditText.customSelectionActionModeCallback = this
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
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
