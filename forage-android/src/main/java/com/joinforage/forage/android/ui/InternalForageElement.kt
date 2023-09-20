package com.joinforage.forage.android.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout

abstract class InternalForageElement(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), ForageElement {

    private var _forageContext: ForageContext? = null
    override fun setForageContext(forageContext: ForageContext) {
        this._forageContext = forageContext
    }

    // internal because submit methods need read-access
    // to the ForageContext but not public because we
    // don't to lull developers into passing a
    // ForagePANEdText instance around as a proxy for
    // the ForageContext value. Seems like an anti-pattern
    internal fun getForageContext(): ForageContext? {
        return _forageContext
    }
}
