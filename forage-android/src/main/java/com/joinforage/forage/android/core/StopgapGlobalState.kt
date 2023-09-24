package com.joinforage.forage.android.core

import com.joinforage.forage.android.ui.ForageConfig

internal object StopgapGlobalState {
    var forageConfig: ForageConfig? = null
    val envConfig: EnvConfig
        get() = EnvConfig.fromForageConfig(forageConfig)
}
