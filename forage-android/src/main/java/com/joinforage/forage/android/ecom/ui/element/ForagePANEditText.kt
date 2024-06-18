package com.joinforage.forage.android.ecom.ui.element

import android.content.Context
import android.util.AttributeSet
import com.joinforage.forage.android.R
import com.joinforage.forage.android.core.ui.element.ForagePanElement

/**
 * A [ForageElement][com.joinforage.forage.android.core.ui.element.ForageElement] that securely
 * collects a customer's card number. You need a [ForagePANEditText]
 * to call the ForageSDK online-only method to
 * [tokenize an EBT Card][com.joinforage.forage.android.ecom.services.ForageSDK.tokenizeEBTCard]
 */
class ForagePANEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.foragePanEditTextStyle
) : ForagePanElement(context, attrs, defStyleAttr)
