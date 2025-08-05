package com.joinforage.android.example.network

import com.joinforage.android.example.network.model.AuthorizeRequest
import com.joinforage.android.example.network.model.CaptureRequest
import com.joinforage.android.example.network.model.PaymentRequest
import com.joinforage.android.example.network.model.PaymentResponse
import com.skydoves.sandwich.ApiResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

interface ForageApi {
    @POST("api/payments/")
    @Headers("API-VERSION: 2023-05-15")
    suspend fun createPayment(
        @Header("Authorization") bearerToken: String,
        @Header("Merchant-Account") fnsNumber: String,
        @Body paymentRequest: PaymentRequest
    ): ApiResponse<PaymentResponse>

    @POST("api/payments/{payment_ref}/authorize/")
    @Headers("API-VERSION: 2023-05-15")
    suspend fun authorizePayment(
        @Header("Authorization") bearerToken: String,
        @Header("Merchant-Account") fnsNumber: String,
        @Path("payment_ref") paymentRef: String,
        @Body authorizeRequest: AuthorizeRequest
    ): ApiResponse<PaymentResponse>

    @POST("api/payments/{payment_ref}/capture_payment/")
    @Headers("API-VERSION: 2023-05-15")
    suspend fun capturePayment(
        @Header("Authorization") bearerToken: String,
        @Header("Merchant-Account") fnsNumber: String,
        @Path("payment_ref") paymentRef: String,
        @Body captureRequest: CaptureRequest
    ): ApiResponse<PaymentResponse>
}
