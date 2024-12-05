package com.joinforage.forage.android.core.services

internal enum class LogService(val serviceName: String, val logPrefix: String) {
    POS("android-pos-sdk", "[POS]"),
    Ecom("android-sdk", "[Ecom]")
}
