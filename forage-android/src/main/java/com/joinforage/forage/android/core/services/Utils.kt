package com.joinforage.forage.android.core.services

import okhttp3.HttpUrl
import org.json.JSONObject

internal fun HttpUrl.Builder.addTrailingSlash(): HttpUrl.Builder {
    return this.addPathSegment("")
}

internal object ForageConstants {

    object Headers {
        const val MERCHANT_ACCOUNT = "Merchant-Account"
        const val IDEMPOTENCY_KEY = "IDEMPOTENCY-KEY"
        const val TRACE_ID = "x-datadog-trace-id"
        const val AUTHORIZATION = "Authorization"
        const val BEARER = "Bearer"
        const val API_VERSION = "API-VERSION"
        const val CONTENT_TYPE = "Content-Type"
        const val SESSION_TOKEN = "Session-Token"
    }

    object RequestBody {
        const val CARD_NUMBER_TOKEN = "card_number_token"

        // POS-only
        const val REASON = "reason"
        const val METADATA = "metadata"
        const val AMOUNT = "amount"
        const val POS_TERMINAL = "pos_terminal"
        const val PROVIDER_TERMINAL_ID = "provider_terminal_id"
    }

    object PathSegment {
        const val API = "api"
        const val PAYMENT_METHODS = "payment_methods"
        const val PAYMENTS = "payments"
        const val REFUNDS = "refunds"
    }
}

internal enum class VaultType(val value: String) {
    FORAGE_VAULT_TYPE("forage");

    override fun toString(): String {
        return value
    }
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

/**
 * [JSONObject.optString] has trouble falling back to `null` and seems to fallback to `"null"` (string) instead
 */
internal fun JSONObject.getStringOrNull(fieldName: String): String? {
    if (!has(fieldName) || isNull(fieldName)) {
        return null
    }
    return optString(fieldName)
}

internal fun JSONObject.hasNonNull(fieldName: String): Boolean {
    return has(fieldName) && !isNull(fieldName)
}
