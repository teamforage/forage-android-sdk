package com.joinforage.forage.android.core.ui.element

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import com.joinforage.forage.android.R
import com.joinforage.forage.android.core.services.EnvConfig
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.services.vault.AbstractVaultSubmitter
import com.joinforage.forage.android.core.ui.element.state.ElementState

/**
 * ⚠️ **Forage developers use this class to manage common attributes across [ForageElement] types.
 * You don't need to use or worry about it!**
 */
abstract class ForageVaultElement<T : ElementState> @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.foragePanEditTextStyle
) : LinearLayout(context, attrs, defStyleAttr), ForageElement<T> {
    internal abstract fun getVaultSubmitter(
        envConfig: EnvConfig,
        logger: Log
    ): AbstractVaultSubmitter
}
