package com.joinforage.forage.android.ecom.services.vault.bt

import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.network.UnknownErrorApiResponse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test

val dummyRawJson = """{"test":"value"}"""
val dummySuccessResponseJson = Result.success<Any?>(dummyRawJson)
val dummyFailureProxyErrorJson = Result.failure<Any?>(Exception("HTTP response body: {\"proxy_error\": \"something went wrong\"}\n"))
val dummyForageErrorJson = Result.failure<Any?>(
    Exception(
        """
        HTTP response body: {"error": {"forage_code": "ebt_error_14", "message": "Invalid card number - Re-enter Transaction"}}
        HTTP response code: 400
        """.trimIndent() + "\n" // extra newline is intentional!!
    )
)
val dummyEmptyError = Result.failure<Any?>(Exception(""))
val dummyParsedForageError = ForageApiResponse.Failure(
    400,
    "ebt_error_14",
    "Invalid card number - Re-enter Transaction"
)

class BTResponseParserTest {

    @Test
    fun `test isNullResponse is always false`() {
        val parser = BTResponseParser(dummySuccessResponseJson)
        assertThat(parser.isNullResponse).isFalse()
    }

    @Test
    fun `vaultErrorMsg is empty str when no error`() {
        val parser = BTResponseParser(dummySuccessResponseJson)
        assertThat(parser.vaultErrorMsg).isNull()
    }

    @Test
    fun `vaultErrorMsg extract error when it's an error`() {
        val exception = Exception("Something went wrong")
        val result = Result.failure<Any?>(exception)
        val parser = BTResponseParser(result)
        assertThat(exception.message).isEqualTo(parser.vaultErrorMsg)
    }

    @Test
    fun `rawResponse is string of the Result`() {
        val parser = BTResponseParser(dummySuccessResponseJson)

        // so the result will look something like
        // "Success({"test": "value"})" since it's a Result instance
        assertThat(parser.rawResponse).isEqualTo(dummySuccessResponseJson.toString())
    }

    @Test
    fun `vaultError is null when response is successful`() {
        val parser = BTResponseParser(dummySuccessResponseJson)
        assertThat(parser.vaultError).isNull()
    }

    @Test
    fun `vaultError is UnknownErrorApiResponse for bodyText with proxy_error`() {
        val parser = BTResponseParser(dummyFailureProxyErrorJson)
        assertThat(parser.vaultError).isEqualTo(UnknownErrorApiResponse)
    }

    @Test
    fun `vaultError is null for bodyText with no proxy_error`() {
        val parser = BTResponseParser(dummyForageErrorJson)
        assertThat(parser.vaultError).isNull()
    }

    @Test
    fun `forageError is null when body_text=null and statusCode=null`() {
        val parser = BTResponseParser(dummyEmptyError)
        assertThat(parser.forageError).isNull()
    }

    @Test
    fun `forageError is successfully parsed from body_text`() {
        val parser = BTResponseParser(dummyForageErrorJson)
        assertThat(parser.forageError).isEqualTo(dummyParsedForageError)
    }

    @Test
    fun `response is null for unsuccessful response`() {
        val parser = BTResponseParser(dummyEmptyError)
        assertThat(parser.successfulResponse).isNull()
    }

    @Test
    fun `successfully parses Map sync response`() {
        val response = Result.success<Any?>(mapOf("test" to "value"))
        val parser = BTResponseParser(response)
        assertThat(parser.successfulResponse).isEqualTo(
            ForageApiResponse.Success(dummyRawJson)
        )
    }

    @Test
    fun `successfully parses String JSON sync response`() {
        val response = Result.success<Any?>(dummyRawJson)
        val parser = BTResponseParser(response)
        assertThat(parser.successfulResponse).isEqualTo(
            ForageApiResponse.Success(dummyRawJson)
        )
    }

    @Test
    fun `successfully parses null empty response from deferred capture`() {
        val response = Result.success<Any?>(null)
        val parser = BTResponseParser(response)
        assertThat(parser.successfulResponse).isEqualTo(
            ForageApiResponse.Success("{}")
        )
    }

    @Test
    fun `successfully parses empty response from deferred capture`() {
        val response = Result.success<Any?>("")
        val parser = BTResponseParser(response)
        assertThat(parser.successfulResponse).isEqualTo(
            ForageApiResponse.Success("{}")
        )
    }

    @Test
    fun `throw UnknownBTResponseException when for unexpected response type`() {
        val response = Result.success<Any?>(42)
        assertThatThrownBy { BTResponseParser(response) }
            .isInstanceOf(UnknownBTResponseException::class.java)
            .hasMessageContaining("42")
    }
}
