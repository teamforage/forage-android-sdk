package com.joinforage.forage.android.network

import com.joinforage.forage.android.network.model.ForageApiResponse
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.suspendCoroutine

internal class EncryptionKeyService(
    private val okHttpClient: OkHttpClient,
    private val httpUrl: HttpUrl
) {
    suspend fun getEncryptionKey(): ForageApiResponse<String> = try {
        getEncryptionToCoroutine()
    } catch (ex: IOException) {
        ForageApiResponse.Failure(message = ex.message.orEmpty())
    }

    private suspend fun getEncryptionToCoroutine(): ForageApiResponse<String> {
        val url = getEncryptionKeyUrl()

        val request: Request = Request.Builder()
            .url(url)
            .get()
            .build()

        return suspendCoroutine { continuation ->
            okHttpClient.newCall(request).enqueue(
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        continuation.resumeWith(Result.failure(e))
                    }

                    override fun onResponse(call: Call, response: Response) {
                        response.use {
                            if (response.isSuccessful.not()) {
                                continuation.resumeWith(
                                    Result.success(
                                        ForageApiResponse.Failure(response.body?.string().orEmpty())
                                    )
                                )
                            } else {
                                continuation.resumeWith(
                                    Result.success(
                                        ForageApiResponse.Success(
                                            response.body?.string().orEmpty()
                                        )
                                    )
                                )
                            }
                        }
                    }
                }
            )
        }
    }

    private fun getEncryptionKeyUrl(): HttpUrl {
        return httpUrl
            .newBuilder()
            .addPathSegment(ForageConstants.PathSegment.ISO_SERVER)
            .addPathSegment(ForageConstants.PathSegment.ENCRYPTION_ALIAS)
            .build()
    }
}
