package com.joinforage.forage.android.core.ui.element

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import com.joinforage.forage.android.R
import com.joinforage.forage.android.core.ui.element.state.ElementState

/**
 * ⚠️ _Forage developers use this class to manage common attributes across the inheritors.
 * You don't need to use or worry about it!_
 * <br></br>
 * The parent [ForageElement] class of [ForagePinElement][com.joinforage.forage.android.core.ui.element.ForagePinElement]
 * and [ForagePinPad][com.joinforage.forage.android.pos.ui.element.ForagePinPad], when the Elements
 * are used with [ForageTerminalSDK][com.joinforage.forage.android.pos.services.ForageTerminalSDK].
 */
abstract class ForageVaultElement<out T : ElementState> @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.foragePanEditTextStyle
) : LinearLayout(context, attrs, defStyleAttr), ForageElement<T>
