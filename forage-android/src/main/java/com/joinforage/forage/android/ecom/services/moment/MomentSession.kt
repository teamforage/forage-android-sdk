package com.joinforage.forage.android.ecom.services.moment

import android.os.Handler
import android.os.Looper
import java.util.UUID

/**
 * One open moment. Connects the suspended `createMoment` call to the
 * [MomentActivity][com.joinforage.forage.android.ecom.ui.moment.MomentActivity] showing it,
 * and makes sure it settles exactly once.
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

    /** Set by the activity while it's showing this moment, so settling can close it */
    var teardown: (() -> Unit)? = null

    private val handler = Handler(Looper.getMainLooper())
    private val loadDeadline = Runnable { settle(MomentResult.Failed(momentLoadTimeoutError)) }
    private var settled = false
    private var isReady = false

    fun startLoadDeadline() {
        handler.postDelayed(loadDeadline, MOMENT_LOAD_DEADLINE_MS)
    }

    /** The page sent `input_ready`. Only the first call does anything. */
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
            MomentEvent.Succeeded -> settle(MomentResult.Completed)
            is MomentEvent.Failed -> settle(MomentResult.Failed(event.error))
            MomentEvent.Closed -> settle(MomentResult.Dismissed)
        }
    }

    /** The customer pressed Back. */
    fun back() {
        settle(MomentResult.Dismissed)
    }

    /** Tears the moment down, then delivers the result. Only the first call does anything. */
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
