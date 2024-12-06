package com.joinforage.forage.android.pos.integration.base64

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class JavaBase64UtilTest {
    @Test
    fun `encode then decode returns same string`() {
        val base64Util = JavaBase64Util()
        val input = "hello"
        val encoded = base64Util.encode(input)
        val decoded = base64Util.decode(encoded)
        assertThat(encoded).isNotEqualTo(decoded)
        assertThat(decoded.toString(Charsets.UTF_8)).isEqualTo(input)
    }
}
