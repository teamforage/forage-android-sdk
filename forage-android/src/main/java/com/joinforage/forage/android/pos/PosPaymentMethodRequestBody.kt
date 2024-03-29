package com.joinforage.forage.android.pos

import com.joinforage.forage.android.network.model.RequestBody
import org.json.JSONObject

internal data class PosPaymentMethodRequestBody(
    val track2Data: String,
    val type: String = "ebt",
    val reusable: Boolean = true
) : RequestBody {
    override fun toJSONObject(): JSONObject {
        val cardObject = JSONObject()
        cardObject.put("track_2_data", track2Data)

        val rootObject = JSONObject()
        rootObject.put("card", cardObject)
        rootObject.put("type", type)
        rootObject.put("reusable", reusable)

        return rootObject
    }
}
