package com.joinforage.forage.android.pos.integration.base64

import com.joinforage.forage.android.pos.services.init.IBase64Util
import java.util.Base64

class JavaBase64Util : IBase64Util {
    override fun encode(input: ByteArray): String =
        Base64.getEncoder().encodeToString(input)

    override fun encode(input: String): String =
        encode(input.toByteArray())

    override fun decode(input: String): ByteArray =
        Base64.getDecoder().decode(input)
}
