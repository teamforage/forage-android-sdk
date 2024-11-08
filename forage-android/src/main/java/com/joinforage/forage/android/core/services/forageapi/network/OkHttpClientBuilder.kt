package com.joinforage.forage.android.core.services.forageapi.network

import com.joinforage.forage.android.core.services.ForageConstants
import okhttp3.Interceptor
import okhttp3.OkHttpClient

internal object OkHttpClientBuilder {
    // use a singleton OkHttpClient to share connection
    // and thread pools across instances. However, we are
    // able to customize the interceptors and headers for
    // derived clients using the `.newBuilder()` pattern
    // (see provideOkHttpClient implementation). The
    // derived instances share the same resources but retrain
    // distinct configurations. See this SO thread
    // https://stackoverflow.com/questions/72348948/okhttp-newbuilder-per-request
    private val singletonClient: OkHttpClient = OkHttpClient()

    fun provideOkHttpClient(
        sessionToken: String,
        merchantId: String? = null,
        idempotencyKey: String? = null,
        traceId: String? = null,
        apiVersion: String = "default"
    ): OkHttpClient = singletonClient.newBuilder()
        .addInterceptor(
            Interceptor { chain ->
                val request = chain.request()
                    .newBuilder()
                    .addHeader(
                        ForageConstants.Headers.AUTHORIZATION,
                        "${ForageConstants.Headers.BEARER} $sessionToken"
                    )
                    .apply {
                        if (chain.request().headers[ForageConstants.Headers.API_VERSION] == null) {
                            addHeader(ForageConstants.Headers.API_VERSION, apiVersion)
                        }
                    }
                    .apply {
                        merchantId?.let {
                            addHeader(ForageConstants.Headers.MERCHANT_ACCOUNT, it)
                        }
                    }
                    .apply {
                        idempotencyKey?.let {
                            addHeader(ForageConstants.Headers.IDEMPOTENCY_KEY, it)
                        }
                    }
                    .apply {
                        traceId?.let {
                            addHeader(ForageConstants.Headers.TRACE_ID, it)
                        }
                    }
                    .build()

                chain.proceed(request)
            }
        ).build()
}
