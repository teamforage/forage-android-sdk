package com.joinforage.android.example.network.interceptors

import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.Response
import java.util.UUID

/**
 * This Interceptor is not used by the ForageSDK
 */
class AuthInterceptor : Interceptor {
    override fun intercept(chain: Chain): Response {
        val request = chain.request()
            .newBuilder()
            .addHeader("IDEMPOTENCY-KEY", UUID.randomUUID().toString())
            .build()

        return chain.proceed(request)
    }
}
