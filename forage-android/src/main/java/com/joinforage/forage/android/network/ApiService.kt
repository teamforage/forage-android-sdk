package com.joinforage.forage.android.network

import com.joinforage.forage.android.network.ForageAPI.ENCRYPTION_KEY_URL
import com.joinforage.forage.android.network.core.get
import okhttp3.Callback

fun getEncryptionKey(
    bearer: String,
    responseCallback: Callback
) {
    get(
        url = ENCRYPTION_KEY_URL,
        bearer = bearer,
        responseCallback = responseCallback
    )
}
