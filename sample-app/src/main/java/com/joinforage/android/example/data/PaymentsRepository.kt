package com.joinforage.android.example.data

import com.joinforage.android.example.network.di.ForageApiModule
import com.joinforage.android.example.network.model.Address
import com.joinforage.android.example.network.model.PaymentRequest
import com.joinforage.android.example.network.model.PaymentResponse
import com.skydoves.sandwich.ApiResponse

class PaymentsRepository(sessionToken: String) {

    @OptIn(ExperimentalStdlibApi::class)
    private val forageApi = ForageApiModule().provideForageApi(sessionToken)

    suspend fun createPayment(
        bearerToken: String,
        fnsNumber: String,
        amount: Long,
        fundingType: String,
        paymentMethod: String,
        description: String,
        metadata: Map<String, String> = mapOf(),
        deliveryAddress: Address,
        isDelivery: Boolean
    ): ApiResponse<PaymentResponse> {
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
                customerId = "android-test-customer-id"
            )
        )
    }
}
