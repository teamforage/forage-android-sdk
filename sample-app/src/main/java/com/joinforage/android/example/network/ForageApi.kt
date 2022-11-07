package com.joinforage.android.example.network

import com.joinforage.android.example.network.model.PaymentRequest
import com.joinforage.android.example.network.model.PaymentResponse
import com.skydoves.sandwich.ApiResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface ForageApi {
    @POST("api/payments/")
    suspend fun createPayment(@Body paymentRequest: PaymentRequest): ApiResponse<PaymentResponse>
}
