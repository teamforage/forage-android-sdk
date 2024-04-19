package com.joinforage.forage.android

import android.content.res.TypedArray
import com.joinforage.forage.android.ui.AbstractForageElement
import com.joinforage.forage.android.core.element.state.ElementState
import com.joinforage.forage.android.core.telemetry.CustomerPerceivedResponseMonitor
import com.joinforage.forage.android.core.telemetry.EventOutcome
import com.joinforage.forage.android.ui.ForageConfig
import com.joinforage.forage.android.network.model.ForageApiResponse
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
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
internal fun HttpUrl.Builder.addPathSegmentsSafe(path: String): HttpUrl.Builder {
    path.split("/").forEach { segment ->
        if (segment.isNotEmpty()) {
            this.addPathSegment(segment)
        }
    }
    return this
}

internal enum class VaultType(val value: String) {
    VGS_VAULT_TYPE("vgs"),
    BT_VAULT_TYPE("basis_theory"),
    FORAGE_VAULT_TYPE("forage");

    override fun toString(): String {
        return value
    }
}


/**
 * Retrieves the ForageConfig for a given ForageElement, or throws an exception if the
 * ForageConfig is not set.
 *
 * @param element A ForageElement instance
 * @return The ForageConfig associated with the ForageElement
 * @throws ForageConfigNotSetException If the ForageConfig is not set for the ForageElement
 */
internal fun <T : ElementState> getForageConfigOrThrow(element: AbstractForageElement<T>): ForageConfig {
    val context = element.getForageConfig()
    return context ?: throw ForageConfigNotSetException(
        """
    The ForageElement you passed did not have a ForageConfig. In order to submit
    a request via Forage SDK, your ForageElement MUST have a ForageConfig.
    Make sure to call myForageElement.setForageConfig(forageConfig: ForageConfig) 
    immediately on your ForageElement 
            """.trimIndent()
    )
}

/**
 * Determines the outcome of a Forage API response,
 * to report the measurement to the Telemetry service.
 *
 * This involves stopping the measurement timer,
 * marking the Metrics event as a success or failure,
 * and if the event is a failure, setting the Forage error code.
 */
internal fun processApiResponseForMetrics(
    apiResponse: ForageApiResponse<String>,
    measurement: CustomerPerceivedResponseMonitor
) {
    measurement.end()
    val outcome = if (apiResponse is ForageApiResponse.Failure) {
        if (apiResponse.errors.isNotEmpty()) {
            measurement.setForageErrorCode(apiResponse.errors[0].code)
        }
        EventOutcome.FAILURE
    } else {
        EventOutcome.SUCCESS
    }
    measurement.setEventOutcome(outcome).logResult()
}
