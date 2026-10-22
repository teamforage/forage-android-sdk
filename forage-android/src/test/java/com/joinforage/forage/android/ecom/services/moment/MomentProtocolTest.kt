package com.joinforage.forage.android.ecom.services.moment

import com.joinforage.forage.android.core.services.ForageConfig
import org.json.JSONObject
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MomentProtocolTest {
    private val moment = Moment.PaymentCollectPin(paymentRef = "abc 123")

    private fun configFor(env: String) = ForageConfig(merchantId = "mid123", sessionToken = "${env}_token")

    @Test
    fun `builds the moment URL for the token's environment`() {
        assertEquals(
            "https://custom-checkout.joinforage.app/payment/collect-pin?paymentRef=abc+123",
            momentLocation(configFor("prod"), moment, null).url
        )
        assertEquals(
            "https://custom-checkout.staging.joinforage.app",
            momentLocation(configFor("staging"), moment, null).origin
        )
        assertEquals(
            "https://custom-checkout.dev.joinforage.app",
            momentLocation(configFor("dev"), moment, null).origin
        )
        assertEquals(
            "http://custom-checkout.joinforage.localhost",
            momentLocation(configFor("local"), moment, null).origin
        )
    }

    @Test
    fun `uses sandbox for a token from an unknown environment`() {
        assertEquals(
            "https://custom-checkout.sandbox.joinforage.app",
            momentLocation(configFor("unknown"), moment, null).origin
        )
    }

    @Test
    fun `uses only the origin of a base URL override`() {
        val location = momentLocation(configFor("dev"), moment, "http://10.0.2.2:3000/ignored/path")

        assertEquals("http://10.0.2.2:3000", location.origin)
        assertEquals("http://10.0.2.2:3000/payment/collect-pin?paymentRef=abc+123", location.url)
        assertEquals("http://10.0.2.2:3000", momentLocation(configFor("local"), moment, "http://10.0.2.2:3000").origin)
    }

    @Test
    fun `ignores the base URL override outside of dev and local`() {
        for (env in listOf("sandbox", "cert", "staging", "prod", "mock", "unknown")) {
            val location = momentLocation(configFor(env), moment, "http://10.0.2.2:3000")
            assertEquals(momentLocation(configFor(env), moment, null), location, env)
        }
    }

    @Test
    fun `sends the config in the init message`() {
        val payload = JSONObject(momentInitMessage(configFor("dev"), "4.2.2")).let {
            assertEquals("initialize_iframe", it.getString("name"))
            it.getJSONObject("payload")
        }

        assertEquals("mid123", payload.getString("merchantId"))
        assertEquals("dev_token", payload.getString("sessionToken"))
        assertEquals("dev", payload.getString("env"))
        assertEquals("moment", payload.getString("usecase"))
        assertEquals("forage-android-sdk", payload.getString("sdkName"))
        assertEquals("4.2.2", payload.getString("sdkVersion"))
    }

    @Test
    fun `parses events from the page`() {
        assertEquals(MomentEvent.Hello, parseMomentEvent("""{"type":"input_hello"}"""))
        assertEquals(MomentEvent.Ready, parseMomentEvent("""{"type":"input_ready","usecase":"moment"}"""))
        assertEquals(MomentEvent.Closed, parseMomentEvent("""{"type":"moment_closed"}"""))
        assertEquals(MomentEvent.Succeeded, parseMomentEvent("""{"type":"submit_success"}"""))
        assertEquals(
            MomentEvent.Succeeded,
            parseMomentEvent("""{"type":"submit_success","payload":{"ref":"abc"}}""")
        )

        val failed = parseMomentEvent(
            """{"type":"submit_failure","payload":{"code":"resource_not_found","httpStatusCode":404,"message":"nope"}}"""
        ) as MomentEvent.Failed
        assertEquals("resource_not_found", failed.error.code)
        assertEquals(404, failed.error.httpStatusCode)
        assertEquals("nope", failed.error.message)
    }

    @Test
    fun `treats a failure payload that isn't a ForageError as unknown_server_error`() {
        val failed = parseMomentEvent("""{"type":"submit_failure","payload":"oops"}""") as MomentEvent.Failed

        assertEquals("unknown_server_error", failed.error.code)
        assertEquals(500, failed.error.httpStatusCode)
    }

    @Test
    fun `ignores unknown and malformed messages`() {
        assertNull(parseMomentEvent("""{"type":"something_new"}"""))
        assertNull(parseMomentEvent("not json"))
    }

    @Test
    fun `reduces URLs to their origin`() {
        assertEquals("https://custom-checkout.dev.joinforage.app", originOf("https://Custom-Checkout.dev.joinforage.app/x?y=z"))
        assertEquals("http://10.0.2.2:3000", originOf("http://10.0.2.2:3000"))
        assertNull(originOf("not a url"))
    }
}
