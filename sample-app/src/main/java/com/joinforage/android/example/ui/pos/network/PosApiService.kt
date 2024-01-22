package com.joinforage.android.example.ui.pos.network

import com.joinforage.android.example.ui.pos.data.Merchant
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers

private const val BASE_URL = "https://api.dev.joinforage.app"
const val AUTH_TOKEN = "AUTH_TOKEN"

private val moshi = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())
    .build()

private val retrofit = Retrofit.Builder()
    .baseUrl(BASE_URL)
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .build()

interface PosApiService {
    @Headers("Authorization: Bearer $AUTH_TOKEN")
    @GET("api/merchants/")
    suspend fun getMerchantInfo(@Header("Merchant-Account") merchantId: String): Merchant
}

object PosApi {
    val retrofitService: PosApiService by lazy {
        retrofit.create(PosApiService::class.java)
    }
}
