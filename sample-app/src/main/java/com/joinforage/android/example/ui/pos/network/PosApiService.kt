package com.joinforage.android.example.ui.pos.network

import com.joinforage.android.example.ui.pos.data.Merchant
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers

private const val BASE_URL = "https://api.dev.joinforage.app/api/"

private val retrofit = Retrofit.Builder()
    .addConverterFactory(MoshiConverterFactory.create())
    .baseUrl(BASE_URL)
    .build()

interface PosApiService {
    @Headers("Authorization: Bearer dev_FoMa7j4TTML6Tc2WG4cBDhTFwwOEOM")
    @GET("merchants")
    suspend fun getMerchantInfo(@Header("merchantAccount") merchantId: String): Merchant
}

object PosApi {
    val retrofitService : PosApiService by lazy {
        retrofit.create(PosApiService::class.java)
    }
}