package com.joinforage.forage.android.ecom.services.moment

import android.os.Handler
import android.os.Looper
import com.joinforage.forage.android.core.services.forageapi.network.error.ForageError
import com.joinforage.forage.android.ecom.services.CreateMomentParams
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Everything [createMoment][com.joinforage.forage.android.ecom.services.ForageSDK.createMoment]
 * does, apart from the parts that need a real device, so tests can run it.
 *
 * @param isSupported Whether this device's WebView supports moments. Only checked once the
 * params are valid.
 * @param launch Shows the moment with the given session ID, i.e. starts the
 * [MomentActivity][com.joinforage.forage.android.ecom.ui.moment.MomentActivity].
 */
internal suspend fun openMoment(
    params: CreateMomentParams,
    telemetry: MomentTelemetry,
    isSupported: () -> Boolean,
    mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    launch: (sessionId: String) -> Unit
): MomentResult {
    val (forageConfig, moment) = params
    telemetry.called()

    // failures before the moment opens are logged here, and the session logs the rest
    fun failBeforeOpening(error: ForageError) =
        MomentResult.Failed(error).also { telemetry.settled(it, reachedReady = false) }

    if (moment.params.values.any { it.isBlank() }) {
        return failBeforeOpening(
            ForageError(400, "user_error", "Invalid moment params provided for ${moment.name}")
        )
    }
    // the bridge relies on addWebMessageListener, which can restrict which origins and
    // frames reach it. older WebViews don't have it, and there's no safe fallback
    if (!isSupported()) {
        return failBeforeOpening(
            ForageError(500, "unknown_server_error", "This device's WebView doesn't support moments")
        )
    }

    return withContext(mainDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val session = MomentSession(
                location = momentLocation(forageConfig, moment),
                initMessage = momentInitMessage(forageConfig, forageConfig.envConfig.PUBLISH_VERSION),
                telemetry = telemetry
            ) { result ->
                if (continuation.isActive) continuation.resume(result)
            }

            if (!MomentSessions.open(session)) {
                continuation.resume(
                    failBeforeOpening(ForageError(400, "user_error", "Another moment is already open"))
                )
                return@suspendCancellableCoroutine
            }

            // the caller's coroutine is cancelled, so it doesn't get a result, but the moment
            // still closes and is recorded as dismissed
            continuation.invokeOnCancellation {
                Handler(Looper.getMainLooper()).post {
                    session.settle(MomentResult.Dismissed)
                }
            }
            session.startLoadDeadline()
            launch(session.id)
        }
    }
}
