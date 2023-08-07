package com.joinforage.forage.android.ui

import android.app.Application
import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
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
) : ForageUI, LinearLayout(context, attrs, defStyleAttr) {
    private var vault: VaultWrapper

    init {
        // Must initialize DD at the beginning of each render function. DD requires the context,
        // so we need to wait until a context is present to run initialization code. However,
        // we have logging all over the SDK that relies on the render happening first.
        val logger = Log.getInstance()
        logger.initializeDD(context)

        setWillNotDraw(false)
        orientation = VERTICAL

        val vaultType = LDManager.getVaultProvider(context.applicationContext as Application, logger)
        vault = if (vaultType == VaultConstants.BT_VAULT_TYPE) {
            BTVaultWrapper(context, attrs, defStyleAttr)
        } else {
            VGSVaultWrapper(context, attrs, defStyleAttr)
        }
        addView(vault.getUnderlying())
        addView(getLogoImageViewLayout(context))
        logger.i("ForagePINEditText successfully rendered")
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

    override var isValid: Boolean = vault?.isValid ?: false
    override var isEmpty: Boolean = vault?.isEmpty ?: true
    override fun setTextColor(textColor: Int) {
        vault.setTextColor(textColor)
    }
    override fun setTextSize(textSize: Float) {
        vault.setTextSize(textSize)
    }

    override var typeface: Typeface? = vault.typeface
    override fun setHint(hint: String) {
        vault.setHint(hint)
    }
    override fun setHintTextColor(hintTextColor: Int) {
        vault.setHintTextColor(hintTextColor)
    }
}
