package com.joinforage.forage.android.core.services.vault

internal interface ISecurePinCollector {
    fun clearText()
    fun isComplete(): Boolean
}
