package com.joinforage.forage.android.ecom.services.vault.bt

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class BtFailureResponseRegExpTest {

    @Test
    fun `bodyText contains match for valid message`() {
        val message = "Exception message... HTTP response body: This is the body\n ..."
        val result = Result.failure<Any>(Exception(message))
        val regExp = BtFailureResponseRegExp(result)
        assertThat(regExp.bodyText).isEqualTo("This is the body")
    }

    @Test
    fun `statusCode contains int match for valid message`() {
        val message = "Exception message... HTTP response code: 400\n ..."
        val result = Result.failure<Any>(Exception(message))
        val regExp = BtFailureResponseRegExp(result)
        assertThat(regExp.statusCode).isEqualTo(400)
    }

    @Test
    fun `bodyText groupValues get(1) does not throw on no match`() {
        val message = "Exception message... "
        val result = Result.failure<Any>(Exception(message))
        val regExp = BtFailureResponseRegExp(result)
        assertThat(regExp.bodyText).isNull()
    }

    @Test
    fun `statusCode groupValues get(1) does not throw on no match`() {
        val message = "Exception message... "
        val result = Result.failure<Any>(Exception(message))
        val regExp = BtFailureResponseRegExp(result)
        assertThat(regExp.statusCode).isNull()
    }

    @Test
    fun `bodyText is null for Result Success`() {
        val result = Result.success<Any?>(null)
        val regExp = BtFailureResponseRegExp(result)
        assertThat(regExp.bodyText).isNull()
    }

    @Test
    fun `statusCode is null for Result Success`() {
        val result = Result.success<Any?>(null)
        val regExp = BtFailureResponseRegExp(result)
        assertThat(regExp.statusCode).isNull()
    }

    @Test
    fun `containsProxyError is false for Result Success`() {
        val result = Result.success<Any?>(null)
        val regExp = BtFailureResponseRegExp(result)
        assertThat(regExp.containsProxyError).isFalse()
    }

    @Test
    fun `containsProxyError is false when no proxy_error in error`() {
        val message = "Exception message... HTTP response body: {\"blah_error\": \"This is the body\"}\n ..."
        val result = Result.failure<Any>(Exception(message))
        val regExp = BtFailureResponseRegExp(result)
        assertThat(regExp.containsProxyError).isFalse()
    }

    @Test
    fun `containsProxyError is true when proxy_error in error`() {
        val message = "Exception message... HTTP response body: {\"proxy_error\": \"This is the body\"}\n ..."
        val result = Result.failure<Any>(Exception(message))
        val regExp = BtFailureResponseRegExp(result)
        assertThat(regExp.containsProxyError).isTrue()
    }
}
