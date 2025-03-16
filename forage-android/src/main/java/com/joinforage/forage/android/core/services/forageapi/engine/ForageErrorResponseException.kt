package com.joinforage.forage.android.core.services.forageapi.engine

import com.joinforage.forage.android.core.services.forageapi.network.error.ForageError

/**
 * An error that serves as a vehicle to bubble up
 * ForageErrors to the calling code. We don't care about
 * capturing any info besides the forageError since
 * this is an expected flow.
 */
internal class ForageErrorResponseException(
    val forageError: ForageError
) : Exception()
