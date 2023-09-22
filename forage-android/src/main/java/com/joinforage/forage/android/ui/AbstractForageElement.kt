package com.joinforage.forage.android.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import com.joinforage.forage.android.core.StopgapGlobalState

/**
 * Exception thrown when attempting to call pass a reference to a
 * ForageElement without first setting its ForageConfig via
 * `.setForageConfig()`.
 */
class ForageConfigNotSetException() : IllegalStateException(
    """
    You are attempting invoke a method a ForageElement before setting
    it's ForageConfig. Make sure to call
    myForageElement.setForageConfig(forageConfig: ForageConfig) 
    immediately on your ForageElement before you call any other methods.
    """.trimIndent()
)

abstract class AbstractForageElement(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), ForageElement {

    private var _forageConfig: ForageConfig? = null
    override fun setForageConfig(forageConfig: ForageConfig) {
        this._forageConfig = forageConfig

        // TODO: 9/20/23: This is a temporary workaround and is
        //  not meant to stick around. See this doc for more details
        //  https://www.notion.so/joinforage/226d8ee6f8294d2694b1bb451791960b
        StopgapGlobalState.forageConfig = forageConfig
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
