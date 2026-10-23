package com.joinforage.forage.android.ecom.services.moment

import android.os.Looper
import com.joinforage.forage.android.core.logger.InMemoryLogger
import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.network.error.ForageError
import com.joinforage.forage.android.core.services.telemetry.LogAttributes
import com.joinforage.forage.android.core.services.telemetry.Loggable
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class MomentSessionTest {
    private val forageConfig = ForageConfig(merchantId = "mid123", sessionToken = MOMENT_TEST_SESSION_TOKEN)
    private val logger = InMemoryLogger(LogAttributes(forageConfig, traceId = "trace"))
    private var nowMs = 1_000L
    private val telemetry = MomentTelemetry(logger, "payment_collect_pin") { nowMs }

    private val calls = mutableListOf<String>()
    private val results = mutableListOf<MomentResult>()
    private val sessions = mutableListOf<MomentSession>()

    private fun session() = MomentSession(
        location = MomentLocation("https://custom-checkout.dev.joinforage.app", "https://custom-checkout.dev.joinforage.app/x"),
        initMessage = "{}",
        telemetry = telemetry
    ) { result ->
        calls.add("onSettled")
        results.add(result)
    }.also {
        it.teardown = { calls.add("teardown") }
        sessions.add(it)
    }

    private fun advanceBy(ms: Long) {
        nowMs += ms
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
    }

    private fun metrics(name: String) = logger.logs
        .filterIsInstance<Loggable.Metric>()
        .filter { it.attrs["event_name"] == name }

    @After
    fun closeSessions() {
        // MomentSessions is a process-wide singleton, so don't leak an open session into other tests
        sessions.forEach { MomentSessions.remove(it) }
    }

    @Test
    fun `settles once`() {
        val session = session()

        session.settle(MomentResult.Completed)
        session.settle(MomentResult.Dismissed)
        session.settle(MomentResult.Failed(ForageError(500, "unknown_server_error", "nope")))

        assertEquals(listOf<MomentResult>(MomentResult.Completed), results)
        assertEquals(1, metrics("moment_outcome").size)
    }

    @Test
    fun `replies to hello with the init message`() {
        val replies = mutableListOf<String>()

        session().handle(MomentEvent.Hello) { replies.add(it) }

        assertEquals(listOf("{}"), replies)
        assertTrue(results.isEmpty())
    }

    @Test
    fun `maps page events to results`() {
        val error = ForageError(404, "resource_not_found", "nope")
        val cases = listOf(
            MomentEvent.Succeeded to MomentResult.Completed,
            MomentEvent.Failed(error) to MomentResult.Failed(error),
            MomentEvent.Closed to MomentResult.Dismissed
        )

        for ((event, expected) in cases) {
            val session = session()
            MomentSessions.open(session)
            session.handle(event) { throw AssertionError("shouldn't reply to $event") }

            assertEquals(expected, results.last(), "$event")
            assertNull(MomentSessions.get(session.id))
        }
    }

    @Test
    fun `ready from the page cancels the load deadline`() {
        val session = session()
        session.startLoadDeadline()

        session.handle(MomentEvent.Ready) {}
        advanceBy(MOMENT_LOAD_DEADLINE_MS)

        assertTrue(results.isEmpty())
    }

    @Test
    fun `back dismisses the moment`() {
        session().back()

        assertEquals(listOf<MomentResult>(MomentResult.Dismissed), results)
    }

    @Test
    fun `tears down before delivering the result`() {
        session().settle(MomentResult.Dismissed)

        assertEquals(listOf("teardown", "onSettled"), calls)
    }

    @Test
    fun `fails with timeout_error when the page isn't ready by the load deadline`() {
        val session = session()
        session.startLoadDeadline()

        advanceBy(MOMENT_LOAD_DEADLINE_MS - 1)
        assertTrue(results.isEmpty())

        advanceBy(1)
        val error = (results.single() as MomentResult.Failed).error
        assertEquals("timeout_error", error.code)
        assertEquals(504, error.httpStatusCode)
        assertEquals("teardown", calls.first())
        assertNull(session.teardown)
    }

    @Test
    fun `ready cancels the load deadline`() {
        val session = session()
        session.startLoadDeadline()

        advanceBy(1_000)
        session.ready()
        advanceBy(MOMENT_LOAD_DEADLINE_MS * 2)

        assertTrue(results.isEmpty())
    }

    @Test
    fun `only one session can be open at a time`() {
        val first = session()
        val second = session()

        assertTrue(MomentSessions.open(first))
        assertFalse(MomentSessions.open(second))
        assertSame(first, MomentSessions.get(first.id))
        assertNull(MomentSessions.get(second.id))
        assertNull(MomentSessions.get("wrong-id"))
        assertNull(MomentSessions.get(null))

        first.settle(MomentResult.Dismissed)

        assertNull(MomentSessions.get(first.id))
        assertTrue(MomentSessions.open(second))
    }

    @Test
    fun `logs when the page is ready, once`() {
        val session = session()

        advanceBy(1_234)
        session.ready()
        advanceBy(100)
        session.ready()

        val ready = metrics("moment_ready").single()
        assertEquals("1234.0", ready.attrs["response_time_ms"])
        assertEquals("payment_collect_pin", ready.attrs["moment_name"])
        assertEquals("create_moment", ready.attrs["action"])
    }

    @Test
    fun `logs a completed moment`() {
        val session = session()
        advanceBy(500)
        session.ready()
        advanceBy(2_000)
        session.settle(MomentResult.Completed)

        val outcome = metrics("moment_outcome").single()
        assertEquals("success", outcome.attrs["event_outcome"])
        assertEquals("2500.0", outcome.attrs["response_time_ms"])
        assertEquals("true", outcome.attrs["moment_reached_ready"])
        assertEquals("payment_collect_pin", outcome.attrs["moment_name"])
        assertEquals("metric", outcome.attrs["log_type"])
        assertNull(outcome.attrs["http_status"])
        assertNull(outcome.attrs["forage_error_code"])
        assertTrue(logger.logs.any { it is Loggable.Info && it.msg.startsWith("[Moment] Completed payment_collect_pin") })
        assertTrue(logger.logs.none { it is Loggable.Error })
    }

    @Test
    fun `logs a dismissed moment`() {
        val session = session()
        advanceBy(300)
        session.settle(MomentResult.Dismissed)

        val outcome = metrics("moment_outcome").single()
        assertEquals("dismissed", outcome.attrs["event_outcome"])
        assertNull(outcome.attrs["http_status"])
        assertEquals("300.0", outcome.attrs["response_time_ms"])
        assertEquals("false", outcome.attrs["moment_reached_ready"])
        assertTrue(logger.logs.none { it is Loggable.Error })
    }

    @Test
    fun `logs a failed moment as an error with its code`() {
        val session = session()
        session.startLoadDeadline()
        advanceBy(MOMENT_LOAD_DEADLINE_MS)

        val outcome = metrics("moment_outcome").single()
        assertEquals("failure", outcome.attrs["event_outcome"])
        assertEquals("timeout_error", outcome.attrs["forage_error_code"])
        assertEquals("504", outcome.attrs["http_status"])
        assertEquals("false", outcome.attrs["moment_reached_ready"])
        val error = logger.logs.filterIsInstance<Loggable.Error>().single()
        assertTrue(error.msg.contains("timeout_error (504)"), error.msg)
    }

    @Test
    fun `never logs the session token or moment params`() {
        val telemetry = MomentTelemetry(logger, Moment.PaymentCollectPin("secret-ref").name) { nowMs }
        telemetry.called()
        telemetry.ready()
        telemetry.settled(MomentResult.Failed(ForageError(400, "user_error", "bad")), reachedReady = true)

        val logged = logger.logs.joinToString { "${it.msg} ${it.attrs.values}" }
        assertFalse(logged.contains(MOMENT_TEST_SESSION_TOKEN))
        assertFalse(logged.contains("secret-ref"))
    }

    @Test
    fun `logs nothing without a logger`() {
        val session = MomentSession(
            location = MomentLocation("https://a.example", "https://a.example/x"),
            initMessage = "{}",
            telemetry = MomentTelemetry(null, "payment_collect_pin")
        ) { results.add(it) }

        session.ready()
        session.settle(MomentResult.Completed)

        assertEquals(listOf<MomentResult>(MomentResult.Completed), results)
    }
}
