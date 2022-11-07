package com.joinforage.forage.android.network

import com.joinforage.forage.android.BuildConfig

internal object ForageAPI {
    private const val BASE_URL = BuildConfig.BASE_URL
    const val TOKENIZE_URL = "${BASE_URL}api/payment_methods/"
    const val ENCRYPTION_KEY_URL = "${BASE_URL}iso_server/encryption_alias/"
}
