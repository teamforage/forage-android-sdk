package com.joinforage.forage.android.network

import com.joinforage.forage.android.core.DDManager
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

internal class MessageStatusService(
    private val httpUrl: HttpUrl,
    okHttpClient: OkHttpClient
) : NetworkService(okHttpClient) {
    private val internalLogger = DDManager.getLogger()
    suspend fun getStatus(contentId: String): ForageApiResponse<String> = try {
        internalLogger.i("GET request for Encryption Key")
        getStatusToCoroutine(contentId)
    } catch (ex: IOException) {
        internalLogger.e("Failed while trying to GET Encryption Key", ex)
        ForageApiResponse.Failure(listOf(ForageError(500, "unknown_server_error", ex.message.orEmpty())))
    }

    private suspend fun getStatusToCoroutine(contentId: String): ForageApiResponse<String> {
        val url = getMessageStatusUrl(contentId)

        val request: Request = Request.Builder()
            .url(url)
            .header(ForageConstants.Headers.API_VERSION, "2023-02-01")
            .get()
            .build()

        return convertCallbackToCoroutine(request)
    }

    private fun getMessageStatusUrl(contentId: String): HttpUrl {
        return httpUrl
            .newBuilder()
            .addPathSegment(ForageConstants.PathSegment.API)
            .addPathSegment(ForageConstants.PathSegment.MESSAGE)
            .addPathSegment(contentId)
            .build()
    }
}
