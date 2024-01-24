package com.joinforage.android.example.ui.pos.network

import com.joinforage.android.example.network.model.PaymentResponse
import com.joinforage.android.example.ui.pos.data.Merchant
import com.joinforage.android.example.ui.pos.data.PosPaymentRequest
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

private const val BASE_URL = "https://api.dev.joinforage.app"

private val moshi = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())
    .build()

private val retrofit = Retrofit.Builder()
    .baseUrl(BASE_URL)
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .build()

interface PosApiService {
    @GET("api/merchants/")
    suspend fun getMerchantInfo(
        @Header("Authorization") authorization: String,
        @Header("Merchant-Account") merchantId: String
    ): Merchant

    @POST("api/payments/")
    suspend fun createPayment(
        @Header("Authorization") authorization: String,
        @Header("Merchant-Account") merchantId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body payment: PosPaymentRequest
    ): PaymentResponse
}

object PosApi {
    val retrofitService: PosApiService by lazy {
        retrofit.create(PosApiService::class.java)
    }
}

fun formatAuthHeader(sessionToken: String) = "Bearer $sessionToken"
