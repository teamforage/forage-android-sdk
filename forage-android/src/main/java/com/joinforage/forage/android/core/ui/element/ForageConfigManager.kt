package com.joinforage.forage.android.core.ui.element

internal class ForageConfigManager(
    // designated method that subclass can run all the
    // side-effect things exactly once the first time
    // setForageConfig is called. Side effect include
    // initializing logger module, feature flag module,
    // and view UI manipulation logic
    private val initWithForageConfig: (forageConfig: ForageConfig) -> Unit
) {
    internal var forageConfig: ForageConfig? = null
        set(newForageConfig) {
            // only accept non-null forage configs
            if (newForageConfig == null) return

            // keep a record of whether this was the first time
            // setForageConfig is getting called. we'll use
            // this info later
            val isFirstCallToSet = field == null

            // update the forage config
            field = newForageConfig

            // there are a number of side effect operations that we
            // need to run as soon as a ForageElement has access to
            // ForageConfig data. However, we don't want to run these
            // operations on any subsequent calls to setForageConfig
            // or else that could crash the app.
            if (isFirstCallToSet) {
                initWithForageConfig(newForageConfig)
            } else {
                // TODO: possible opportunity to log that
                //  they tried to do sessionToken refreshing
            }
        }
}
