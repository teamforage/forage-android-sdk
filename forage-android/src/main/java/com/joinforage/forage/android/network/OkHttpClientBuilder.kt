package com.joinforage.forage.android.network

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
        traceId: String? = null
    ): OkHttpClient = singletonClient.newBuilder()
        .addInterceptor(
            Interceptor { chain ->
                val request = chain.request()
                    .newBuilder()
                    .addHeader(
                        ForageConstants.Headers.AUTHORIZATION,
                        "${ForageConstants.Headers.BEARER} $sessionToken"
                    )
                    .run {
                        // If the API_VERSION header has already been appended, don't override it!
                        chain.request().headers[ForageConstants.Headers.API_VERSION]?.let {
                            this
                        }
                        // Otherwise, set the default API_VERSION header
                            ?: addHeader(
                                ForageConstants.Headers.API_VERSION,
                                "default"
                            )
                    }
                    .run {
                        merchantId?.let {
                            addHeader(
                                ForageConstants.Headers.MERCHANT_ACCOUNT,
                                merchantId
                            )
                        }
                            ?: this
                    }
                    .run {
                        idempotencyKey?.let {
                            addHeader(
                                ForageConstants.Headers.IDEMPOTENCY_KEY,
                                idempotencyKey
                            )
                        }
                            ?: this
                    }
                    .run {
                        traceId?.let {
                            addHeader(
                                ForageConstants.Headers.TRACE_ID,
                                traceId
                            )
                        }
                            ?: this
                    }
                    .build()

                chain.proceed(request)
            }
        ).build()
}
