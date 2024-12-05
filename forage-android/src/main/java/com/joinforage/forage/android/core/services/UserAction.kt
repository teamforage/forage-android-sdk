package com.joinforage.forage.android.core.services

internal enum class UserAction(val value: String) {
    BALANCE("balance"),
    CAPTURE("capture"),
    DEFER_CAPTURE("defer_capture"),
    REFUND("refund"),
    DEFER_REFUND("defer_refund");
}
