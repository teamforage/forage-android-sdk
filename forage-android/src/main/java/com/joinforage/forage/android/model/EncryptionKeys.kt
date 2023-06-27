package com.joinforage.forage.android.model

import org.json.JSONObject

internal data class EncryptionKeys(
    val vgsAlias: String,
    val btAlias: String
) {
    object ModelMapper {
        fun from(string: String): EncryptionKeys {
            val jsonObject = JSONObject(string)

            val vgsAlias = jsonObject.getString("alias")
            val btAlias = jsonObject.getString("bt_alias")
            return EncryptionKeys(
                vgsAlias = vgsAlias,
                btAlias = btAlias
            )
        }
    }
}
