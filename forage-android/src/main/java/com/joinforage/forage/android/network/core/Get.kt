package com.joinforage.forage.android.network.core

import okhttp3.Callback
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

fun get(
    url: String,
    bearer: String,
    responseCallback: Callback
) {
    val okHttpClient = OkHttpClient().newBuilder()
        .addInterceptor(
            object : Interceptor {
                override fun intercept(chain: Interceptor.Chain): Response {
                    val request = chain.request()
                        .newBuilder()
                        .addHeader(
                            "Authorization",
                            "Bearer $bearer"
                        )
                        .build()

                    return chain.proceed(request)
                }
            }
        ).build()

    val request: Request = Request.Builder()
        .url(url)
        .get()
        .build()

    okHttpClient.newCall(request).enqueue(responseCallback)
}
