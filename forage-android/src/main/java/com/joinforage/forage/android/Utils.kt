package com.joinforage.forage.android

import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import com.joinforage.forage.android.network.model.SQSError
import okhttp3.HttpUrl
import kotlin.random.Random

/**
 * We generate a random jitter amount to add to our retry delay when polling for the status of
 * Payments and Payment Methods so that we can avoid a thundering herd scenario in which there are
 * several requests retrying at the same exact time.
 *
 * Returns a random integer between -25 and 25
 */
internal fun getJitterAmount(random: Random = Random.Default): Int {
    return random.nextInt(-25, 26)
}

internal fun sqsMessageToError(sqsError: SQSError): ForageApiResponse.Failure {
    val forageError = ForageError(
        sqsError.statusCode,
        sqsError.forageCode,
        sqsError.message,
        sqsError.details
    )
    return ForageApiResponse.Failure(listOf(forageError))
}

internal fun HttpUrl.Builder.addTrailingSlash(): HttpUrl.Builder {
    return this.addPathSegment("")
}
