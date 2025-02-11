package com.joinforage.forage.android.core.services.telemetry

import android.util.Base64

class AndroidBase64Util : IBase64Util {
    override fun encode(input: ByteArray): String =
        Base64.encodeToString(input, Base64.DEFAULT)

    override fun encode(input: String): String =
        encode(input.toByteArray())

    override fun decode(input: String): ByteArray =
        Base64.decode(input, Base64.DEFAULT)
}
