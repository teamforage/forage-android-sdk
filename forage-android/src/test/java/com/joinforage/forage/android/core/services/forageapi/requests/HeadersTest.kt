package com.joinforage.forage.android.core.services.forageapi.requests

import org.junit.Test
import kotlin.test.assertContains

class HeadersTest {
    @Test
    fun `verify SDK version header`() {
        val headers = Headers()
        assertContains(headers.toMap(), "X-Forage-Android-Sdk-Version")
    }
}
