package com.joinforage.android.example.network

import com.joinforage.android.example.network.model.PaymentRequest
import com.joinforage.android.example.network.model.PaymentResponse
import com.skydoves.sandwich.ApiResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface ForageApi {
    @POST("api/payments/")
    @Headers("API-VERSION: 2023-05-15")
    suspend fun createPayment(@Header("Authorization") bearerToken: String, @Header("Merchant-Account") fnsNumber: String, @Body paymentRequest: PaymentRequest): ApiResponse<PaymentResponse>
}
