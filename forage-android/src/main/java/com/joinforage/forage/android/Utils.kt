package com.joinforage.forage.android

import android.content.res.TypedArray
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

internal fun HttpUrl.Builder.addTrailingSlash(): HttpUrl.Builder {
    return this.addPathSegment("")
}

internal fun TypedArray.getBoxCornerRadius(styleIndex: Int, defaultBoxCornerRadius: Float): Float {
    val styledBoxCornerRadius = getDimension(styleIndex, 0f)
    return if (styledBoxCornerRadius == 0f) defaultBoxCornerRadius else styledBoxCornerRadius
}

// This extension splits the path by "/" and adds each segment individually to the path.
// This is to prevent the URL from getting corrupted through internal OKHttp URL encoding.
fun HttpUrl.Builder.addPathSegmentsSafe(path: String): HttpUrl.Builder {
    path.split("/").forEach { segment ->
        if (segment.isNotEmpty()) {
            this.addPathSegment(segment)
        }
    }
    return this
}
