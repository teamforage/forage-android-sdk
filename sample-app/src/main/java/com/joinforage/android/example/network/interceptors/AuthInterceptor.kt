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
            "prod" -> PROD_BEARER_TOKEN
            "cert" -> CERT_BEARER_TOKEN
            else -> SANDBOX_BEARER_TOKEN
        }

        private fun getMerchantAccount() = when (BuildConfig.FLAVOR) {
            "prod" -> PROD_MERCHANT_ACCOUNT
            "cert" -> CERT_MERCHANT_ACCOUNT
            else -> SANDBOX_MERCHANT_ACCOUNT
        }

        private const val SANDBOX_BEARER_TOKEN = "<INSERT_OAUTH_TOKEN>"
        private const val SANDBOX_MERCHANT_ACCOUNT = "<INSERT_MERCHANT_ACCOUNT>"

        private const val CERT_BEARER_TOKEN = "<INSERT_OAUTH_TOKEN>"
        private const val CERT_MERCHANT_ACCOUNT = "<INSERT_MERCHANT_ACCOUNT>"

        private const val PROD_BEARER_TOKEN = "<INSERT_OAUTH_TOKEN>"
        private const val PROD_MERCHANT_ACCOUNT = "<INSERT_MERCHANT_ACCOUNT>"
    }
}
