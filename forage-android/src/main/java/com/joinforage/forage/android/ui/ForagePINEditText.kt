package com.joinforage.forage.android.ui

import android.app.Application
import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import com.basistheory.android.view.TextElement
import com.joinforage.forage.android.LDManager
import com.joinforage.forage.android.R
import com.joinforage.forage.android.VaultConstants
import com.joinforage.forage.android.collect.BTPinCollector
import com.joinforage.forage.android.collect.PinCollector
import com.joinforage.forage.android.collect.VGSPinCollector
import com.joinforage.forage.android.core.Log
import com.joinforage.forage.android.core.element.SimpleElementListener
import com.joinforage.forage.android.core.element.StatefulElementListener
import com.joinforage.forage.android.core.element.state.ElementState
import com.verygoodsecurity.vgscollect.widget.VGSEditText

class ForagePINEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.foragePanEditTextStyle
) : AbstractForageElement(context, attrs, defStyleAttr) {
    private val _linearLayout: LinearLayout
    private val btVaultWrapper: BTVaultWrapper
    private val vgsVaultWrapper: VGSVaultWrapper
    private var _SET_ONLY_vault: VaultWrapper? = null
    private val vault: VaultWrapper
        get() {
            if (_SET_ONLY_vault == null)
                throw ForageConfigNotSetException()
            return _SET_ONLY_vault!!
        }

    init {
        context.obtainStyledAttributes(attrs, R.styleable.ForagePINEditText, defStyleAttr, 0)
            .apply {
                try {
                    setWillNotDraw(false)
                    orientation = VERTICAL
                    gravity = Gravity.CENTER

                    btVaultWrapper = BTVaultWrapper(context, attrs, defStyleAttr)
                    vgsVaultWrapper = VGSVaultWrapper(context, attrs, defStyleAttr)
                    // ensure both wrappers init with the
                    // same typeface (or the attributes)
                    btVaultWrapper.typeface = vgsVaultWrapper.typeface

                    val elementWidth: Int = getDimensionPixelSize(R.styleable.ForagePINEditText_elementWidth, ViewGroup.LayoutParams.MATCH_PARENT)
                    val elementHeight: Int = getDimensionPixelSize(R.styleable.ForagePINEditText_elementHeight, ViewGroup.LayoutParams.WRAP_CONTENT)

                    _linearLayout = LinearLayout(context)
                    _linearLayout.layoutParams = ViewGroup.LayoutParams(elementWidth, elementHeight)
                    _linearLayout.orientation = VERTICAL
                    _linearLayout.gravity = Gravity.CENTER
                } finally {
                    recycle()
                }
            }
    }

    override fun setForageConfig(forageConfig: ForageConfig) {
        // super is responsible for initializing the log and some
        // global state so it must be called first
        super.setForageConfig(forageConfig)

        // Must initialize DD at the beginning of each render function. DD requires the context,
        // so we need to wait until a context is present to run initialization code. However,
        // we have logging all over the SDK that relies on the render happening first.
        val logger = Log.getInstance()
        logger.initializeDD(context)

        val vaultType = LDManager.getVaultProvider(context.applicationContext as Application, logger)
        _SET_ONLY_vault = if (vaultType == VaultConstants.BT_VAULT_TYPE) {
            btVaultWrapper
        } else {
            vgsVaultWrapper
        }

        _linearLayout.addView(vault.getUnderlying())
        _linearLayout.addView(getLogoImageViewLayout(context))
        addView(_linearLayout)

        logger.i("[UIView] ForagePINEditText successfully rendered")
    }

    override fun clearText() {
        vault.clearText()
    }

    // While the events that ForageElements expose mirrors the
    // blur, focus, change etc events of an Android view,
    // they represent different abstractions. Our users need to
    // interact with the ForageElement abstraction and not the
    // implementation details of which Android view we use.
    // Therefore we expose novel set listener methods instead of
    // overriding the convention setOn*Listener
    override fun setOnFocusEventListener(l: SimpleElementListener) {
        vault.setOnFocusEventListener(l)
    }
    override fun setOnBlurEventListener(l: SimpleElementListener) {
        vault.setOnBlurEventListener(l)
    }
    override fun setOnChangeEventListener(l: StatefulElementListener) {
        vault.setOnChangeEventListener(l)
    }

    override fun getElementState(): ElementState {
        return vault.manager.getState()
    }

    internal fun getCollector(
        merchantAccount: String
    ): PinCollector {
        if (vault is BTVaultWrapper) {
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
        return vault.getVGSEditText()
    }

    internal fun getTextElement(): TextElement {
        return vault.getTextElement()
    }

    override fun setTextColor(textColor: Int) {
        vault.setTextColor(textColor)
    }
    override fun setTextSize(textSize: Float) {
        vault.setTextSize(textSize)
    }

    override var typeface: Typeface?
        get() = if (vault == btVaultWrapper) btVaultWrapper.typeface else vgsVaultWrapper.typeface
        set(value) {
            // keep all vault providers in sync regardless of
            // whether they were added to the UI
            btVaultWrapper.typeface = value
            vgsVaultWrapper.typeface = value
        }
    override fun setHint(hint: String) {
        vault.setHint(hint)
    }
    override fun setHintTextColor(hintTextColor: Int) {
        vault.setHintTextColor(hintTextColor)
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
}
