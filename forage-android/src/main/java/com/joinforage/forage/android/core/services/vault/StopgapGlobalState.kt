package com.joinforage.forage.android.core.services.vault

import com.joinforage.forage.android.core.services.EnvConfig
import com.joinforage.forage.android.core.ui.element.ForageConfig

internal object StopgapGlobalState {
    var forageConfig: ForageConfig? = null
    val envConfig: EnvConfig
        get() = EnvConfig.fromForageConfig(forageConfig)
}
