package com.joinforage.forage.android.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import com.joinforage.forage.android.core.StopgapGlobalState
import com.joinforage.forage.android.core.element.state.ElementState

/**
 * ⚠️ Forage developers use this class to manage common attributes across [ForageElement] types.
 * You don't need to use or worry about it!
 */
abstract class AbstractForageElement<T : ElementState>(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), ForageElement<T> {

    private var _forageConfig: ForageConfig? = null

    // designated method that subclass can run all the
    // side-effect things exactly once the first time
    // setForageConfig is called. Side effect include
    // initializing logger module, feature flag module,
    // and view UI manipulation logic
    protected abstract fun initWithForageConfig(forageConfig: ForageConfig)

    override fun setForageConfig(forageConfig: ForageConfig) {
        // keep a record of whether this was the first time
        // setForageConfig is getting called. we'll use
        // this info later
        val isFirstCallToSet = _forageConfig == null

        // update the forage config
        this._forageConfig = forageConfig

        // TODO: 9/20/23: This is a temporary workaround and is
        //  not meant to stick around. See this doc for more details
        //  https://www.notion.so/joinforage/226d8ee6f8294d2694b1bb451791960b
        StopgapGlobalState.forageConfig = forageConfig

        // there are a number of side effect operations that we
        // need to run as soon as a ForageElement has access to
        // ForageConfig data. However, we don't want to run these
        // operations on any subsequent calls to setForageConfig
        // or else that could crash the app.
        if (isFirstCallToSet) {
            initWithForageConfig(forageConfig)
        } else {
            // TODO: possible opportunity to log that
            //  they tried to do sessionToken refreshing
        }
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
