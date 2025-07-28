package com.joinforage.forage.android.core.services.vault

interface ISecurePinCollector {
    fun getPin(): String
    fun clearText()
    fun isComplete(): Boolean
}
