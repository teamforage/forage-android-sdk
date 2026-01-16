package com.joinforage.android.example.data

import com.joinforage.android.example.network.di.ForageApiModule
import com.joinforage.android.example.network.model.Address
import com.joinforage.android.example.network.model.AuthorizeRequest
import com.joinforage.android.example.network.model.CaptureRequest
import com.joinforage.android.example.network.model.PaymentRequest
import com.joinforage.android.example.network.model.PaymentResponse
import retrofit2.Response

class PaymentsRepository(sessionToken: String) {

    @OptIn(ExperimentalStdlibApi::class)
    private val forageApi = ForageApiModule().provideForageApi(sessionToken)

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
    ): Response<PaymentResponse> {
        val bearerString = "Bearer $bearerToken"
        return forageApi.createPayment(
            bearerString,
            fnsNumber,
            PaymentRequest(
                amount = amount,
                fundingType = fundingType,
                paymentMethod = paymentMethod,
                description = description,
                metadata = metadata,
                deliveryAddress = deliveryAddress,
                isDelivery = isDelivery,
                customerId = "android-test-customer-id-2"
            )
        )
    }

    suspend fun authorizePayment(
        bearerToken: String,
        fnsNumber: String,
        paymentRef: String,
        requestPartialAuthorization: Boolean
    ): Response<PaymentResponse> {
        val bearerString = "Bearer $bearerToken"
        return forageApi.authorizePayment(
            bearerString,
            fnsNumber,
            paymentRef,
            AuthorizeRequest(requestPartialAuthorization)
        )
    }

    suspend fun capturePayment(
        bearerToken: String,
        fnsNumber: String,
        paymentRef: String,
        captureAmount: String,
        productList: List<CaptureRequest.Product>
    ): Response<PaymentResponse> {
        val bearerString = "Bearer $bearerToken"
        return forageApi.capturePayment(
            bearerString,
            fnsNumber,
            paymentRef,
            CaptureRequest(
                captureAmount = captureAmount,
                qualifiedHealthcareTotal = captureAmount,
                qualifiedHealthcareSubtotal = captureAmount,
                productList = productList
            )
        )
    }
}
