package com.joinforage.forage.android.core.services

import org.json.JSONObject


internal enum class VaultType(val value: String) {
    FORAGE_VAULT_TYPE("forage");

    override fun toString(): String {
        return value
    }
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
