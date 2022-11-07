package com.joinforage.forage.android.network.core

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

fun post(
    url: String,
    json: String,
    merchantAccount: String,
    bearer: String,
    responseCallback: Callback,
    idempotencyKey: String? = null
) {
    postCall(
        requestBuilder = Request.Builder().url(url),
        json = json,
        merchantAccount = merchantAccount,
        bearerToken = bearer,
        idempotencyKey = idempotencyKey
    ).enqueue(responseCallback)
}

internal fun postCall(
    requestBuilder: Request.Builder,
    json: String,
    merchantAccount: String,
    bearerToken: String,
    idempotencyKey: String? = null
): Call {
    val okHttpClient = OkHttpClient().newBuilder()
        .addInterceptor(
            getRequestInterceptor(
                merchantAccount = merchantAccount,
                bearerToken = bearerToken,
                idempotencyKey = idempotencyKey
            )
        ).build()

    val body: RequestBody =
        json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

    val request = requestBuilder
        .post(body)
        .build()

    return okHttpClient.newCall(request)
}
