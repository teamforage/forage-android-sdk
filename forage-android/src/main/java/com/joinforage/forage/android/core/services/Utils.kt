package com.joinforage.forage.android.core.services

import org.json.JSONObject
import kotlin.random.Random

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

fun generateTraceId(): String {
    // Seed the random number generator with current time
    val random = Random(System.currentTimeMillis())
    val length = 14
    return "44" + (1..length).map { random.nextInt(10) }.joinToString("")
}

internal fun JSONObject.toMap(): Map<String, String> = keys().asSequence().associateWith { get(it).toString() }
