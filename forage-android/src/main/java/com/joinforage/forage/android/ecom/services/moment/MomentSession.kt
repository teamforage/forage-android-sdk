package com.joinforage.forage.android.ecom.services.moment

import android.os.Handler
import android.os.Looper
import java.util.UUID

/**
 * One open moment. Connects the suspended `createMoment` call to the
 * [MomentActivity][com.joinforage.forage.android.ecom.ui.moment.MomentActivity] showing it,
 * and makes sure it settles.
 *
 * Everything here runs on the main thread.
 */
internal class MomentSession(
    val location: MomentLocation,
    val initMessage: String,
    private val telemetry: MomentTelemetry,
    private val onSettled: (MomentResult) -> Unit
) {
    val id: String = UUID.randomUUID().toString()

    var teardown: (() -> Unit)? = null

    private val handler = Handler(Looper.getMainLooper())
    private val loadDeadline = Runnable { settle(MomentResult.Failed(MomentProtocol.LOAD_TIMEOUT_ERROR)) }
    private var settled = false
    private var isReady = false

    fun startLoadDeadline() {
        handler.postDelayed(loadDeadline, MomentProtocol.LOAD_DEADLINE_MS)
    }

    fun ready() {
        if (settled || isReady) return
        isReady = true
        handler.removeCallbacks(loadDeadline)
        telemetry.ready()
    }

    /**
     * Handles an event from the page.
     *
     * @param reply Sends a message back to the page that sent the event.
     */
    fun handle(event: MomentEvent, reply: (String) -> Unit) {
        when (event) {
            MomentEvent.Hello -> reply(initMessage)
            MomentEvent.Ready -> ready()
            MomentEvent.Success -> settle(MomentResult.Completed)
            is MomentEvent.Failure -> settle(MomentResult.Failed(event.error))
            MomentEvent.Closed -> settle(MomentResult.Dismissed)
        }
    }

    /** The customer triggered the native Android Back behavior on their device. */
    fun back() {
        settle(MomentResult.Dismissed)
    }

    fun settle(result: MomentResult) {
        if (settled) return
        settled = true
        handler.removeCallbacks(loadDeadline)
        MomentSessions.remove(this)
        teardown?.invoke()
        teardown = null
        telemetry.settled(result, reachedReady = isReady)
        onSettled(result)
    }
}

/** The open moment, if any. Only one moment can be open at a time. */
internal object MomentSessions {
    private var current: MomentSession? = null

    @Synchronized
    fun open(session: MomentSession): Boolean {
        if (current != null) return false
        current = session
        return true
    }

    @Synchronized
    fun get(id: String?): MomentSession? = current?.takeIf { it.id == id }

    @Synchronized
    fun remove(session: MomentSession) {
        if (current === session) current = null
    }
}
