package com.joinforage.forage.android.network

import com.joinforage.forage.android.BuildConfig
import com.joinforage.forage.android.core.Log
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

internal class EncryptionKeyService(
    private val httpUrl: HttpUrl,
    okHttpClient: OkHttpClient
) : NetworkService(okHttpClient) {
    private val internalLogger = Log.getInstance(!BuildConfig.DEBUG)
    suspend fun getEncryptionKey(): ForageApiResponse<String> = try {
        internalLogger.i("GET request for Encryption Key")
        getEncryptionToCoroutine()
    } catch (ex: IOException) {
        internalLogger.e("Failed while trying to GET Encryption Key", ex)
        ForageApiResponse.Failure(listOf(ForageError(500, "unknown_server_error", ex.message.orEmpty())))
    }

    private suspend fun getEncryptionToCoroutine(): ForageApiResponse<String> {
        val url = getEncryptionKeyUrl()

        val request: Request = Request.Builder()
            .url(url)
            .get()
            .build()

        return convertCallbackToCoroutine(request)
    }

    private fun getEncryptionKeyUrl(): HttpUrl {
        return httpUrl
            .newBuilder()
            .addPathSegment(ForageConstants.PathSegment.ISO_SERVER)
            .addPathSegment(ForageConstants.PathSegment.ENCRYPTION_ALIAS)
            .build()
    }
}
