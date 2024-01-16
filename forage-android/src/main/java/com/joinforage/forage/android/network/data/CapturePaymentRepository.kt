package com.joinforage.forage.android.network.data

import com.joinforage.forage.android.collect.PinCollector
import com.joinforage.forage.android.model.EncryptionKeys
import com.joinforage.forage.android.model.Payment
import com.joinforage.forage.android.model.PaymentMethod
import com.joinforage.forage.android.network.EncryptionKeyService
import com.joinforage.forage.android.network.PaymentMethodService
import com.joinforage.forage.android.network.PaymentService
import com.joinforage.forage.android.network.PollingService
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.Message

internal class CapturePaymentRepository(
    private val pinCollector: PinCollector,
    private val encryptionKeyService: EncryptionKeyService,
    private val pollingService: PollingService,
    private val paymentService: PaymentService,
    private val paymentMethodService: PaymentMethodService
) {
    suspend fun capturePayment(paymentRef: String): ForageApiResponse<String> {
        return when (val response = encryptionKeyService.getEncryptionKey()) {
            is ForageApiResponse.Success -> getPaymentMethodFromPayment(
                paymentRef = paymentRef,
                encryptionKey = pinCollector.parseEncryptionKey(
                    EncryptionKeys.ModelMapper.from(response.data)
                )
            )
            else -> response
        }
    }

    private suspend fun getPaymentMethodFromPayment(
        paymentRef: String,
        encryptionKey: String
    ): ForageApiResponse<String> {
        return when (val response = paymentService.getPayment(paymentRef)) {
            is ForageApiResponse.Success -> getTokenFromPaymentMethod(
                paymentRef = paymentRef,
                paymentMethodRef = Payment.ModelMapper.from(response.data).paymentMethod,
                encryptionKey = encryptionKey
            )
            else -> response
        }
    }

    private suspend fun getTokenFromPaymentMethod(
        paymentRef: String,
        paymentMethodRef: String,
        encryptionKey: String
    ): ForageApiResponse<String> {
        return when (val response = paymentMethodService.getPaymentMethod(paymentMethodRef)) {
            is ForageApiResponse.Success -> submitPaymentCapture(
                paymentRef = paymentRef,
                vaultRequestParams = BaseVaultRequestParams(
                    cardNumberToken = pinCollector.parseVaultToken(
                        PaymentMethod.ModelMapper.from(response.data)
                    ),
                    encryptionKey = encryptionKey
                )
            )
            else -> response
        }
    }

    private suspend fun submitPaymentCapture(
        paymentRef: String,
        vaultRequestParams: BaseVaultRequestParams
    ): ForageApiResponse<String> {
        val response = pinCollector.submitPaymentCapture(
            paymentRef = paymentRef,
            vaultRequestParams
        )

        return when (response) {
            is ForageApiResponse.Success -> {
                pollingCapturePaymentMessageStatus(
                    Message.ModelMapper.from(response.data).contentId,
                    paymentRef
                )
            }
            else -> response
        }
    }

    private suspend fun pollingCapturePaymentMessageStatus(
        contentId: String,
        paymentRef: String
    ): ForageApiResponse<String> {
        val pollingResponse = pollingService.execute(
            contentId = contentId,
            operationDescription = "payment capture of Payment $paymentRef"
        )
        return when (pollingResponse) {
            is ForageApiResponse.Success -> {
                paymentService.getPayment(paymentRef = paymentRef)
            }
            else -> pollingResponse
        }
    }

    companion object {
        private fun ForageApiResponse<String>.getStringResponse() = when (this) {
            is ForageApiResponse.Failure -> this.errors[0].message
            is ForageApiResponse.Success -> this.data
        }
    }
}
