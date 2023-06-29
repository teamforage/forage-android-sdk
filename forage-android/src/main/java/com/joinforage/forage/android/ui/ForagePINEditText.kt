package com.joinforage.forage.android.ui

import android.app.Application
import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.widget.LinearLayout
import com.basistheory.android.view.TextElement
import com.joinforage.forage.android.core.DDManager
import com.joinforage.forage.android.LDManager
import com.joinforage.forage.android.R
import com.joinforage.forage.android.VaultConstants
import com.joinforage.forage.android.collect.BTPinCollector
import com.joinforage.forage.android.collect.PinCollector
import com.joinforage.forage.android.collect.VGSPinCollector
import com.verygoodsecurity.vgscollect.widget.VGSEditText

class ForagePINEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.foragePanEditTextStyle
) : ForageUI, LinearLayout(context, attrs, defStyleAttr) {
    private var vault: VaultWrapper?

    init {
        val logger = DDManager.initializeLogger(context)

        setWillNotDraw(false)
        orientation = VERTICAL

        var vaultType = LDManager.getVaultProvider(context.applicationContext as Application)
        vault = if (vaultType == VaultConstants.BT_VAULT_TYPE) {
            BTVaultWrapper(context, attrs, defStyleAttr)
        } else {
            VGSVaultWrapper(context, attrs, defStyleAttr)
        }
        addView(vault!!.getUnderlying())
        addView(getLogoImageViewLayout(context))
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
        return vault?.getVGSEditText()!!
    }

    internal fun getTextElement(): TextElement {
        return vault?.getTextElement()!!
    }

    override var isValid: Boolean = vault?.isValid ?: false
    override var isEmpty: Boolean = vault?.isEmpty ?: true
    override fun setTextColor(textColor: Int) {
        vault?.setTextColor(textColor)
    }
    override fun setTextSize(textSize: Float) {
        vault?.setTextSize(textSize)
    }

    override var typeface: Typeface? = vault?.typeface
    override fun setHint(hint: String) {
        vault?.setHint(hint)
    }
    override fun setHintTextColor(hintTextColor: Int) {
        vault?.setHintTextColor(hintTextColor)
    }
}
