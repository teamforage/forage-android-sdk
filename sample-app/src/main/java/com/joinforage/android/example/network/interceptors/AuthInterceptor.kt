package com.joinforage.android.example.network.interceptors

import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.Response
import java.util.UUID
import javax.inject.Inject

/**
 * This Interceptor is not used by the ForageSDK
 */
class AuthInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Chain): Response {
        val request = chain.request()
            .newBuilder()
            .addHeader("Authorization", "Bearer $DEVELOPMENT_BEARER_TOKEN")
            .addHeader("IDEMPOTENCY-KEY", UUID.randomUUID().toString())
            .addHeader("Merchant-Account", DEVELOPMENT_MERCHANT_ACCOUNT)
            .build()

        return chain.proceed(request)
    }

    companion object {
        private const val DEVELOPMENT_BEARER_TOKEN = "AbCaccesstokenXyz"
        private const val DEVELOPMENT_MERCHANT_ACCOUNT = "8000009"
    }
}
