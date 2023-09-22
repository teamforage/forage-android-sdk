package com.joinforage.forage.android.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout

abstract class AbstractForageElement(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), ForageElement {

    private var _forageConfig: ForageConfig? = null
    override fun setForageConfig(forageConfig: ForageConfig) {
        this._forageConfig = forageConfig
    }

    // internal because submit methods need read-access
    // to the ForageConfig but not public because we
    // don't to lull developers into passing a
    // ForagePANEdiText instance around as a proxy for
    // the ForageConfig value. Seems like an anti-pattern
    internal fun getForageConfig(): ForageConfig? {
        return _forageConfig
    }
}
