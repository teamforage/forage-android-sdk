package com.joinforage.forage.android.model

import org.json.JSONObject

internal data class EncryptionKey(
    val vgsAlias: String,
    val btAlias: String
) {
    object ModelMapper {
        fun from(string: String): EncryptionKey {
            val jsonObject = JSONObject(string)

            val vgsAlias = jsonObject.getString("alias")
            val btAlias = jsonObject.getString("bt_alias")
            return EncryptionKey(
                vgsAlias = vgsAlias,
                btAlias = btAlias
            )
        }
    }
}
