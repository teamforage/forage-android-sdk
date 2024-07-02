package com.joinforage.forage.android.pos.services.encryption.iso4

import com.joinforage.forage.android.pos.services.encryption.AesBlock
import kotlin.random.Random

internal interface Iso4Field {
    fun toAesBlock(): AesBlock
}

internal fun makeRandomHexString(length: Int): String {
    val bytes = ByteArray(length / 2) // 2 hex chars per byte
    Random.nextBytes(bytes)
    return bytes.joinToString("") { "%02x".format(it) }
}
