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
        amount: Long,
        fundingType: String,
        paymentMethod: String,
        description: String,
        metadata: Map<String, String> = mapOf(),
        deliveryAddress: Address,
        isDelivery: Boolean
    ): ApiResponse<PaymentResponse> {
        return forageApi.createPayment(
            PaymentRequest(
                amount = amount,
                fundingType = fundingType,
                paymentMethod = paymentMethod,
                description = description,
                metadata = metadata,
                deliveryAddress = deliveryAddress,
                isDelivery = isDelivery
            )
        )
    }
}
