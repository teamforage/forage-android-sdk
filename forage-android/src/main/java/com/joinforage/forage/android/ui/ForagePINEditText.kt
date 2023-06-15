package com.joinforage.forage.android.ui

import android.app.Application
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.text.InputType
import android.util.AttributeSet
import android.widget.LinearLayout
import com.basistheory.android.view.TextElement
import com.google.android.material.textfield.TextInputLayout
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
) : LinearLayout(context, attrs, defStyleAttr) {
    private var vaultType: String
    private var vault: PINVaultTextField?

    init {
        setWillNotDraw(false)
        orientation = VERTICAL

//        vaultType = LDManager.getVaultProvider(context.applicationContext as Application)
        vaultType = VaultConstants.VGS_VAULT_TYPE
        if (vaultType == VaultConstants.BT_VAULT_TYPE) {
            vault = BTVaultWrapper(context, attrs, defStyleAttr)
        } else if (vaultType == VaultConstants.VGS_VAULT_TYPE) {
            vault = VGSVaultWrapper(context, attrs, defStyleAttr)
        } else {
            vault = VGSVaultWrapper(context, attrs, defStyleAttr)
//            throw Error("This shouldn't be possible!!")
        }
        addView(vault!!.getUnderlying())
        addView(getLogoImageViewLayout(context))
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

    // TODO: These should probably be nullable
    internal fun getTextInputEditText(): VGSEditText {
        return vault?.getVGSEditText()!!
    }

    internal fun getTextElement(): TextElement {
        return vault?.getTextElement()!!
    }
}
