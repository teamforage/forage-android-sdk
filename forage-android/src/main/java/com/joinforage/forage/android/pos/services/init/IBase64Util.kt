package com.joinforage.forage.android.pos.services.init

internal interface IBase64Util {
    fun encode(input: ByteArray): String
    fun encode(input: String): String
    fun decode(input: String): ByteArray
}
