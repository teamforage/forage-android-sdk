package com.joinforage.android.example.network.interceptors

import com.joinforage.android.example.BuildConfig
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
            .addHeader("Authorization", "Bearer ${getBearerToken()}")
            .addHeader("IDEMPOTENCY-KEY", UUID.randomUUID().toString())
            .addHeader("Merchant-Account", getMerchantAccount())
            .build()

        return chain.proceed(request)
    }

    companion object {
        private fun getBearerToken() = when (BuildConfig.FLAVOR) {
            "dev" -> DEV_BEARER_TOKEN
            else -> SANDBOX_BEARER_TOKEN
        }

        private fun getMerchantAccount() = when (BuildConfig.FLAVOR) {
            "dev" -> DEV_MERCHANT_ACCOUNT
            else -> SANDBOX_MERCHANT_ACCOUNT
        }

        private const val SANDBOX_BEARER_TOKEN = "AbCaccesstokenXyz"
        private const val SANDBOX_MERCHANT_ACCOUNT = "8000009"

        private const val DEV_BEARER_TOKEN = "AbCaccesstokenXyz"
        private const val DEV_MERCHANT_ACCOUNT = "9876545"
    }
}
