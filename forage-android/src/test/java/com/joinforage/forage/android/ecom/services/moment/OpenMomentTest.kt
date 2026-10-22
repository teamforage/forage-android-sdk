package com.joinforage.forage.android.ecom.services.moment

import android.os.Looper
import com.joinforage.forage.android.core.logger.InMemoryLogger
import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.telemetry.LogAttributes
import com.joinforage.forage.android.core.services.telemetry.Loggable
import com.joinforage.forage.android.ecom.services.CreateMomentParams
import com.joinforage.forage.android.mock.TestForagePaymentSheet.Companion.MOCK_SESSION_TOKEN
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class OpenMomentTest {
    private val forageConfig = ForageConfig(merchantId = "mid123", sessionToken = MOCK_SESSION_TOKEN)
    private val logger = InMemoryLogger(LogAttributes(forageConfig, traceId = "trace"))
    private val launched = mutableListOf<String>()

    private fun params(paymentRef: String = "abc123") =
        CreateMomentParams(forageConfig, Moment.PaymentCollectPin(paymentRef = paymentRef))

    private suspend fun open(
        params: CreateMomentParams = params(),
        isSupported: Boolean = true
    ) = openMoment(
        params,
        MomentTelemetry(logger, params.moment.name),
        isSupported = { isSupported },
        mainDispatcher = UnconfinedTestDispatcher()
    ) { launched.add(it) }

    private fun outcomes() = logger.logs
        .filterIsInstance<Loggable.Metric>()
        .filter { it.attrs["event_name"] == "moment_outcome" }

    @After
    fun closeSessions() {
        // MomentSessions is a process-wide singleton, so don't leak an open session into other tests
        launched.forEach { id -> MomentSessions.get(id)?.let { MomentSessions.remove(it) } }
    }

    @Test
    fun `fails with user_error for a blank param without checking WebView support`() = runTest {
        var checkedSupport = false
        val result = openMoment(
            params(paymentRef = " "),
            MomentTelemetry(logger, "payment_collect_pin"),
            isSupported = { checkedSupport = true; true },
            mainDispatcher = UnconfinedTestDispatcher(testScheduler)
        ) { launched.add(it) }

        val error = (result as MomentResult.Failed).error
        assertEquals("user_error", error.code)
        assertEquals(400, error.httpStatusCode)
        assertTrue(!checkedSupport)
        assertTrue(launched.isEmpty())
        assertEquals("failure", outcomes().single().attrs["event_outcome"])
    }

    @Test
    fun `fails when the WebView doesn't support moments`() = runTest {
        val result = open(isSupported = false)

        assertEquals("unknown_server_error", (result as MomentResult.Failed).error.code)
        assertTrue(launched.isEmpty())
        assertEquals("failure", outcomes().single().attrs["event_outcome"])
    }

    @Test
    fun `launches the moment and returns how it ended`() = runTest {
        val call = async(start = CoroutineStart.UNDISPATCHED) { open() }

        val session = assertNotNull(MomentSessions.get(launched.single()))
        session.handle(MomentEvent.Succeeded) {}

        assertEquals(MomentResult.Completed, call.await())
    }

    @Test
    fun `fails with user_error while another moment is open`() = runTest {
        val first = async(start = CoroutineStart.UNDISPATCHED) { open() }

        val second = open()

        val error = (second as MomentResult.Failed).error
        assertEquals("user_error", error.code)
        assertEquals(400, error.httpStatusCode)
        assertEquals(1, launched.size)

        MomentSessions.get(launched.single())?.handle(MomentEvent.Closed) {}
        assertEquals(MomentResult.Dismissed, first.await())
    }

    @Test
    fun `cancelling the caller closes the moment`() = runTest {
        val call = async(start = CoroutineStart.UNDISPATCHED) { open() }
        val session = assertNotNull(MomentSessions.get(launched.single()))
        var tornDown = false
        session.teardown = { tornDown = true }

        call.cancel()
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(call.isCancelled)
        assertTrue(tornDown)
        assertNull(MomentSessions.get(session.id))
        assertEquals("dismissed", outcomes().single().attrs["event_outcome"])
    }
}
