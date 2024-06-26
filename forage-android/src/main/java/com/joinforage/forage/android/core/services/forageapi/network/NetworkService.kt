package com.joinforage.forage.android.core.services.forageapi.network

import com.joinforage.forage.android.core.services.telemetry.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.suspendCoroutine

internal abstract class NetworkService(
    private val okHttpClient: OkHttpClient,
    private val logger: Log
) {
    suspend fun convertCallbackToCoroutine(request: Request) =
        suspendCoroutine { continuation ->
            okHttpClient.newCall(request).enqueue(
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        logger.e("[HTTP] Request failed with exception", throwable = e)
                        continuation.resumeWith(Result.failure(e))
                    }

                    override fun onResponse(call: Call, response: Response) {
                        response.use {
                            if (response.isSuccessful.not()) {
                                val body = response.body
                                if (body != null) {
                                    try {
                                        val parsedError =
                                            ForageApiError.ForageApiErrorMapper.from(body.string())
                                        val error = parsedError.errors[0]
                                        logger.e("[HTTP] Received ${response.code} response from API ${parsedError.path} with message: ${error.message}")

                                        continuation.resumeWith(
                                            Result.success(
                                                ForageApiResponse.Failure.fromError(
                                                    ForageError(
                                                        response.code,
                                                        error.code,
                                                        error.message
                                                    )
                                                )
                                            )
                                        )
                                    } catch (e: Exception) {
                                        logger.e("[HTTP] Received malformed error response from API", throwable = e)
                                        continuation.resumeWith(Result.failure(e))
                                    }
                                } else {
                                    logger.e("[HTTP] Received unknown response from API")
                                    continuation.resumeWith(
                                        Result.success(
                                            UnknownErrorApiResponse
                                        )
                                    )
                                }
                            } else {
                                logger.i("[HTTP] Received ${response.code} response from API ${request.url.encodedPath}")
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
