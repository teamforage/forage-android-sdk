package com.joinforage.android.example.ui.pos.network

import com.joinforage.android.example.network.model.EnvConfig
import com.joinforage.android.example.ui.pos.data.Merchant
import com.joinforage.android.example.ui.pos.data.PosPaymentRequest
import com.joinforage.android.example.ui.pos.data.PosPaymentResponse
import com.joinforage.android.example.ui.pos.data.Refund
import com.joinforage.android.example.ui.pos.data.tokenize.PosPaymentMethod
import com.joinforage.forage.android.pos.PosForageConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

private val moshi = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())
    .build()

interface PosApiService {
    @GET("api/merchants/")
    suspend fun getMerchantInfo(): Merchant

    @POST("api/payments/")
    suspend fun createPayment(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body payment: PosPaymentRequest
    ): PosPaymentResponse

    @POST("api/payments/{paymentRef}/void/")
    suspend fun voidPayment(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Path("paymentRef") paymentRef: String
    ): PosPaymentResponse

    @POST("api/payments/{paymentRef}/refunds/{refundRef}/void/")
    suspend fun voidRefund(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Path("paymentRef") paymentRef: String,
        @Path("refundRef") refundRef: String
    ): Refund

    @POST("api/payment_methods/{paymentMethodRef}/")
    suspend fun reFetchCard(
        @Path("paymentMethodRef") paymentMethodRef: String
    ): PosPaymentMethod

    companion object {
        internal fun from(posForageConfig: PosForageConfig): PosApiService {
            val commonHeadersInterceptor = Interceptor { chain ->
                val newRequest = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer ${posForageConfig.sessionToken}")
                    .addHeader("Merchant-Account", posForageConfig.merchantId)
                    .build()
                chain.proceed(newRequest)
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(commonHeadersInterceptor)
                .build()

            val env = EnvConfig.fromSessionToken(posForageConfig.sessionToken)

            val retrofit = Retrofit.Builder()
                .baseUrl(env.baseUrl)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            return retrofit.create(PosApiService::class.java)
        }
    }
}
