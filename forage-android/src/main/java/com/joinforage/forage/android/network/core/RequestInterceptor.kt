package com.joinforage.forage.android.network.core

import okhttp3.Interceptor

fun getRequestInterceptor(
    merchantAccount: String,
    bearerToken: String,
    idempotencyKey: String? = null
) = Interceptor { chain ->
    val request = chain.request()
        .newBuilder()
        .addHeader(
            "Authorization",
            "Bearer $bearerToken"
        )
        .addHeader(
            "Merchant-Account",
            merchantAccount
        ).run {
            idempotencyKey?.let { addHeader("IDEMPOTENCY-KEY", idempotencyKey) }
                ?: this
        }
        .build()

    chain.proceed(request)
}
