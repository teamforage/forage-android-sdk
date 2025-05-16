package com.joinforage.forage.android.mock

import org.json.JSONArray
import org.json.JSONObject

internal fun getVaultMessageResponse(contentId: String): String {
    return JSONObject().apply {
        put("content_id", contentId)
        put("message_type", "0200")
        put("status", "sent_to_proxy")
        put("failed", false)
        put("errors", JSONArray(emptyList<String>()))
    }.toString()
}
