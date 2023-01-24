package com.joinforage.android.example.network

import com.joinforage.android.example.network.model.PaymentRequest
import com.joinforage.android.example.network.model.PaymentResponse
import com.skydoves.sandwich.ApiResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Header

interface ForageApi {
    @POST("api/payments/")
    suspend fun createPayment(@Header("Authorization") bearerToken: String, @Header("Merchant-Account") fnsNumber: String, @Body paymentRequest: PaymentRequest): ApiResponse<PaymentResponse>
}
