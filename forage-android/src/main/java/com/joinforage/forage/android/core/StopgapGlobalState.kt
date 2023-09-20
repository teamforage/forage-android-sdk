package com.joinforage.forage.android.core

import com.joinforage.forage.android.ui.ForageContext

internal object STOPGAP_GLOBAL_STATE {
    var context: ForageContext? = null

    val FLAVOR: EnvOption
        get() = EnvConfig.fromForageContext(context).FLAVOR
}
