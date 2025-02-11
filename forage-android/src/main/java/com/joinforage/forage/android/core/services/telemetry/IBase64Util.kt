package com.joinforage.forage.android.core.services.telemetry

internal interface IBase64Util {
    fun encode(input: ByteArray): String
    fun encode(input: String): String
    fun decode(input: String): ByteArray
}
