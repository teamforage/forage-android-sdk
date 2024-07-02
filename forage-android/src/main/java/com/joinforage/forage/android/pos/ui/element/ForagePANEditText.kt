package com.joinforage.forage.android.pos.ui.element

import android.content.Context
import android.util.AttributeSet
import com.joinforage.forage.android.R
import com.joinforage.forage.android.core.ui.element.ForagePanElement

/**
 * A [ForageElement] that securely collects a customer's card number. You need a [ForagePANEditText]
 * to call the ForageSDK online-only method to
 * [tokenize an EBT Card][com.joinforage.forage.android.ForageSDK.tokenizeEBTCard], or
 * the ForageTerminalSDK POS method to
 * [tokenize a card][com.joinforage.forage.android.pos.ForageTerminalSDK.tokenizeCard].
 */
class ForagePANEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.foragePanEditTextStyle
) : ForagePanElement(context, attrs, defStyleAttr)
