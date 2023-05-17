package com.joinforage.android.example.data

import com.joinforage.android.example.network.ForageApi
import com.joinforage.android.example.network.model.Address
import com.joinforage.android.example.network.model.PaymentRequest
import com.joinforage.android.example.network.model.PaymentResponse
import com.skydoves.sandwich.ApiResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentsRepository @Inject constructor(
    private val forageApi: ForageApi
) {
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
        var bearerString = "Bearer $bearerToken"
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
