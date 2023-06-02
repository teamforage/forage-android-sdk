package com.joinforage.forage.android.model

import org.json.JSONObject

internal data class EncryptionKey(
    val alias: String
) {
    object ModelMapper {
        fun from(string: String): EncryptionKey {
            val jsonObject = JSONObject(string)

            val alias = jsonObject.getString("alias")
            return EncryptionKey(alias)
        }
    }
}
