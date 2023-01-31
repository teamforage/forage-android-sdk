package com.joinforage.forage.android.network

import com.joinforage.forage.android.network.model.ForageApiResponse
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.suspendCoroutine
import com.joinforage.forage.android.network.model.ForageApiError

abstract class NetworkService(
    private val okHttpClient: OkHttpClient
) {
    suspend fun convertCallbackToCoroutine(request: Request) =
        suspendCoroutine { continuation ->
            okHttpClient.newCall(request).enqueue(
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        continuation.resumeWith(Result.failure(e))
                    }

                    override fun onResponse(call: Call, response: Response) {
                        response.use {
                            if (response.isSuccessful.not()) {
                                val body = response.body
                                if (body != null) {
                                    val parsedError = ForageApiError.ForageApiErrorMapper.from(body.string())
                                    val error = parsedError.errors[0]
                                    continuation.resumeWith(
                                        Result.success(
                                            ForageApiResponse.Failure(response.code, error.code, error.message)
                                        )
                                    )
                                } else {
                                    continuation.resumeWith(
                                        Result.success(
                                            ForageApiResponse.Failure(500, "server_error", "Unknown Server Error")
                                        )
                                    )
                                }
                            } else {
                                continuation.resumeWith(
                                    Result.success(
                                        ForageApiResponse.Success(response.body?.string().orEmpty())
                                    )
                                )
                            }
                        }
                    }
                }
            )
        }
}
