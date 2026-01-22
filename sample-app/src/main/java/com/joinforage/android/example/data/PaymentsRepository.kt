package com.joinforage.android.example.data

import com.joinforage.android.example.network.HttpClient
import com.joinforage.android.example.network.model.Address
import com.joinforage.android.example.network.model.AuthorizeRequest
import com.joinforage.android.example.network.model.CaptureRequest
import com.joinforage.android.example.network.model.EnvConfig
import com.joinforage.android.example.network.model.PaymentRequest
import com.joinforage.android.example.network.model.PaymentResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.squareup.moshi.addAdapter
import okhttp3.OkHttpClient
import java.util.UUID

class PaymentsRepository(sessionToken: String) {

    @OptIn(ExperimentalStdlibApi::class)
    private val moshi = Moshi.Builder()
        .addAdapter(Rfc3339DateJsonAdapter().nullSafe())
        .build()

    private val baseUrl = EnvConfig.fromSessionToken(sessionToken).baseUrl

    private val httpClient = HttpClient(
        baseUrl = baseUrl,
        okHttpClient = OkHttpClient(),
        moshi = moshi
    )

    suspend fun createPayment(
        bearerToken: String,
        fnsNumber: String,
        amount: String,
        fundingType: String,
        paymentMethod: String,
        description: String,
        metadata: Map<String, String> = mapOf(),
        deliveryAddress: Address,
        isDelivery: Boolean
    ): PaymentResponse {
        val headers = mapOf(
            "Authorization" to "Bearer $bearerToken",
            "Merchant-Account" to fnsNumber,
            "API-VERSION" to "2025-05-15",
            "Idempotency-Key" to UUID.randomUUID().toString()
        )

        val request = PaymentRequest(
            amount = amount,
            fundingType = fundingType,
            paymentMethod = paymentMethod,
            description = description,
            metadata = metadata,
            deliveryAddress = deliveryAddress,
            isDelivery = isDelivery,
            customerId = "android-test-customer-id-2"
        )

        return httpClient.post("api/payments/", request, headers)
    }

    suspend fun authorizePayment(
        bearerToken: String,
        fnsNumber: String,
        paymentRef: String,
        requestPartialAuthorization: Boolean
    ): PaymentResponse {
        val headers = mapOf(
            "Authorization" to "Bearer $bearerToken",
            "Merchant-Account" to fnsNumber,
            "API-VERSION" to "2025-05-15",
            "Idempotency-Key" to UUID.randomUUID().toString()
        )

        val request = AuthorizeRequest(requestPartialAuthorization)

        return httpClient.post(
            "api/payments/$paymentRef/authorize/",
            request,
            headers
        )
    }

    suspend fun capturePayment(
        bearerToken: String,
        fnsNumber: String,
        paymentRef: String,
        captureAmount: String,
        productList: List<CaptureRequest.Product>
    ): PaymentResponse {
        val headers = mapOf(
            "Authorization" to "Bearer $bearerToken",
            "Merchant-Account" to fnsNumber,
            "API-VERSION" to "2025-05-15",
            "Idempotency-Key" to UUID.randomUUID().toString()
        )

        val request = CaptureRequest(
            captureAmount = captureAmount,
            qualifiedHealthcareTotal = captureAmount,
            qualifiedHealthcareSubtotal = captureAmount,
            productList = productList
        )

        return httpClient.post("api/payments/$paymentRef/capture_payment/", request, headers)
    }
}
